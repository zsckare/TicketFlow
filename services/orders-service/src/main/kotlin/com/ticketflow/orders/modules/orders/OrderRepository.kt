package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.tickets.TicketInventoryResponse
import com.ticketflow.orders.outbox.OrderConfirmedEvent
import com.ticketflow.orders.outbox.OrderConfirmedItem
import com.ticketflow.orders.outbox.OutboxEventsTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class OrderRepository {
    fun createPending(userId: UUID, inventory: List<TicketInventoryResponse>): OrderResponse = transaction {
        require(inventory.isNotEmpty())
        val currencies = inventory.map { it.currency.uppercase() }.distinct()
        require(currencies.size == 1) { "All order items must use the same currency" }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()
        val amount = inventory.sumOf { BigDecimal(it.price) }
        val first = inventory.first()
        OrdersTable.insert {
            it[OrdersTable.id] = id; it[OrdersTable.userId] = userId
            // Legacy columns remain populated for backwards DB compatibility.
            it[OrdersTable.inventoryId] = UUID.fromString(first.id)
            it[OrdersTable.amount] = amount; it[currency] = currencies.single(); it[discountAmount] = BigDecimal.ZERO; it[promotionCode] = null
            it[status] = OrderStatus.PENDING.name; it[createdAt] = now; it[updatedAt] = now
        }
        inventory.forEach { remote ->
            OrderItemsTable.insert {
                it[OrderItemsTable.id] = UUID.randomUUID(); it[orderId] = id
                it[inventoryId] = UUID.fromString(remote.id); it[eventId] = UUID.fromString(remote.eventId)
                it[sectionId] = remote.sectionId?.let(UUID::fromString); it[seatId] = remote.seatId?.let(UUID::fromString)
                it[unitPrice] = BigDecimal(remote.price); it[currency] = remote.currency.uppercase(); it[status] = "ACTIVE"; it[refundedAmount] = BigDecimal.ZERO; it[discountAmount] = BigDecimal.ZERO; it[createdAt] = now
            }
        }
        findByIdInternal(id)!!
    }

    fun attachReservation(orderId: UUID, inventoryId: UUID, reservationId: UUID, reservedUntil: OffsetDateTime?) = transaction {
        OrderItemsTable.update({ (OrderItemsTable.orderId eq orderId) and (OrderItemsTable.inventoryId eq inventoryId) }) {
            it[OrderItemsTable.reservationId] = reservationId
            it[OrderItemsTable.reservedUntil] = reservedUntil
        }
    }

    fun markReserved(orderId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({ (OrdersTable.id eq orderId) and (OrdersTable.status eq OrderStatus.PENDING.name) }) {
            it[status] = OrderStatus.RESERVED.name; it[failureReason] = null; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            val firstReservation = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.firstOrNull()?.get(OrderItemsTable.reservationId)
            it[reservationId] = firstReservation
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun attachPayment(orderId: UUID, paymentId: UUID): OrderResponse? = transaction {
        val updated = OrdersTable.update({ (OrdersTable.id eq orderId) and (OrdersTable.status eq OrderStatus.RESERVED.name) }) {
            it[OrdersTable.paymentId] = paymentId; it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null else findByIdInternal(orderId)
    }

    fun markConfirmed(
        orderId: UUID,
        userEmail: String,
        confirmedItems: List<OrderConfirmedItem>,
    ): OrderResponse? = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updated = OrdersTable.update({
            (OrdersTable.id eq orderId) and
                (OrdersTable.status eq OrderStatus.RESERVED.name)
        }) {
            it[status] = OrderStatus.CONFIRMED.name
            it[failureReason] = null
            it[updatedAt] = now
        }

        if (updated == 0) return@transaction null

        val order = findByIdInternal(orderId)!!
        val outboxEventId = UUID.randomUUID()
        val event = OrderConfirmedEvent(
            eventId = outboxEventId.toString(),
            occurredAt = now.toString(),
            orderId = order.id,
            userId = requireNotNull(order.userId),
            userEmail = userEmail,
            paymentId = requireNotNull(order.paymentId),
            amount = order.amount,
            currency = order.currency,
            items = confirmedItems,
        )

        OutboxEventsTable.insert {
            it[id] = outboxEventId
            it[aggregateType] = "Order"
            it[aggregateId] = orderId
            it[eventType] = "OrderConfirmed"
            it[topic] = "ticketflow.orders.confirmed"
            it[eventKey] = orderId.toString()
            it[payload] = Json.encodeToString(event)
            it[createdAt] = now
            it[publishedAt] = null
            it[attempts] = 0
            it[lastError] = null
        }

        order
    }

    fun issueTickets(orderId: UUID, userId: UUID): List<IssuedTicketResponse> = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val orderItems = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.toList()
        orderItems.forEach { item ->
            val itemId = item[OrderItemsTable.id]
            val exists = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.orderItemId eq itemId }.any()
            if (!exists) IssuedTicketsTable.insert {
                it[id] = UUID.randomUUID(); it[IssuedTicketsTable.orderId] = orderId; it[orderItemId] = itemId; it[IssuedTicketsTable.userId] = userId
                it[eventId] = item[OrderItemsTable.eventId]; it[inventoryId] = item[OrderItemsTable.inventoryId]; it[sectionId] = item[OrderItemsTable.sectionId]; it[seatId] = item[OrderItemsTable.seatId]
                it[admissionToken] = UUID.randomUUID(); it[status] = IssuedTicketStatus.ISSUED.name; it[issuedAt] = now; it[checkedInAt] = null; it[checkedInByUserId] = null
            }
        }
        findTicketsByOrderInternal(orderId)
    }

    fun findTicketsByUser(userId: UUID): List<IssuedTicketResponse> = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.userId eq userId }.orderBy(IssuedTicketsTable.issuedAt to SortOrder.DESC).map(::toTicket)
    }

    fun findTicketById(ticketId: UUID): IssuedTicketResponse? = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.id eq ticketId }.singleOrNull()?.let(::toTicket)
    }

    fun ticketStats(eventId: UUID): CheckInStatsResponse = transaction {
        val rows = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.eventId eq eventId }.toList()
        val used = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.USED.name }
        val issued = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.ISSUED.name }
        val cancelled = rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.CANCELLED.name }
        CheckInStatsResponse(eventId.toString(), rows.size, used, issued, cancelled)
    }

    fun findTicketByAdmissionToken(token: UUID): IssuedTicketResponse? = transaction {
        IssuedTicketsTable.selectAll().where { IssuedTicketsTable.admissionToken eq token }.singleOrNull()?.let(::toTicket)
    }

    fun checkIn(token: UUID, checkedInByUserId: UUID): IssuedTicketResponse? = transaction {
        val updated = IssuedTicketsTable.update({
            (IssuedTicketsTable.admissionToken eq token) and
                (IssuedTicketsTable.status eq IssuedTicketStatus.ISSUED.name)
        }) {
            it[status] = IssuedTicketStatus.USED.name
            it[checkedInAt] = OffsetDateTime.now(ZoneOffset.UTC)
            it[IssuedTicketsTable.checkedInByUserId] = checkedInByUserId
        }
        if (updated == 0) throw IllegalStateException("Ticket has already been used or cancelled")
        IssuedTicketsTable.selectAll()
            .where { IssuedTicketsTable.admissionToken eq token }
            .single()
            .let(::toTicket)
    }

    fun findAllTickets(): List<IssuedTicketResponse> = transaction {
        IssuedTicketsTable.selectAll()
            .orderBy(IssuedTicketsTable.issuedAt to SortOrder.DESC)
            .map(::toTicket)
    }

    fun cancelIssuedTickets(orderId: UUID) = transaction {
        IssuedTicketsTable.update({ (IssuedTicketsTable.orderId eq orderId) and (IssuedTicketsTable.status eq IssuedTicketStatus.ISSUED.name) }) { it[status] = IssuedTicketStatus.CANCELLED.name }
    }

    private fun findTicketsByOrderInternal(orderId: UUID) = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.orderId eq orderId }.map(::toTicket)
    private fun toTicket(row: ResultRow) = IssuedTicketResponse(
        id = row[IssuedTicketsTable.id].toString(),
        orderId = row[IssuedTicketsTable.orderId].toString(),
        userId = row[IssuedTicketsTable.userId].toString(),
        eventId = row[IssuedTicketsTable.eventId].toString(),
        inventoryId = row[IssuedTicketsTable.inventoryId].toString(),
        sectionId = row[IssuedTicketsTable.sectionId]?.toString(),
        seatId = row[IssuedTicketsTable.seatId]?.toString(),
        admissionToken = row[IssuedTicketsTable.admissionToken].toString(),
        status = IssuedTicketStatus.valueOf(row[IssuedTicketsTable.status]),
        issuedAt = row[IssuedTicketsTable.issuedAt].toString(),
        checkedInAt = row[IssuedTicketsTable.checkedInAt]?.toString(),
        checkedInByUserId = row[IssuedTicketsTable.checkedInByUserId]?.toString(),
    )

    fun markCancelled(orderId: UUID): OrderResponse? = transaction {
        val updated=OrdersTable.update({ (OrdersTable.id eq orderId) and ((OrdersTable.status eq OrderStatus.RESERVED.name) or (OrdersTable.status eq OrderStatus.CONFIRMED.name)) }) {
            it[status]=OrderStatus.CANCELLED.name;it[failureReason]=null;it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)
        }; if(updated==0)null else findByIdInternal(orderId)
    }
    fun markFailed(orderId: UUID, reason:String):OrderResponse=transaction{OrdersTable.update({OrdersTable.id eq orderId}){it[status]=OrderStatus.FAILED.name;it[failureReason]=reason.take(500);it[updatedAt]=OffsetDateTime.now(ZoneOffset.UTC)};findByIdInternal(orderId)!!}
    fun findById(id:UUID)=transaction{findByIdInternal(id)}
    fun findByUser(userId:UUID)=transaction{OrdersTable.selectAll().where{OrdersTable.userId eq userId}.orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    fun findAll()=transaction{OrdersTable.selectAll().orderBy(OrdersTable.createdAt to SortOrder.DESC).map(::toResponse)}
    fun findExpiredReserved(now: OffsetDateTime): List<OrderResponse> = transaction {
        val expiredOrderIds = OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.reservedUntil lessEq now }
            .map { it[OrderItemsTable.orderId] }
            .distinct()
        if (expiredOrderIds.isEmpty()) emptyList()
        else OrdersTable.selectAll()
            .where { (OrdersTable.id inList expiredOrderIds) and (OrdersTable.status eq OrderStatus.RESERVED.name) }
            .map(::toResponse)
    }
    fun allTicketStatuses(): List<IssuedTicketStatus> = transaction { IssuedTicketsTable.selectAll().map { IssuedTicketStatus.valueOf(it[IssuedTicketsTable.status]) } }
    private fun findByIdInternal(id:UUID)=OrdersTable.selectAll().where{OrdersTable.id eq id}.singleOrNull()?.let(::toResponse)
    private fun items(orderId:UUID)=OrderItemsTable.selectAll().where{OrderItemsTable.orderId eq orderId}.map{row->OrderItemResponse(id=row[OrderItemsTable.id].toString(), inventoryId=row[OrderItemsTable.inventoryId].toString(), reservationId=row[OrderItemsTable.reservationId]?.toString(), reservedUntil=row[OrderItemsTable.reservedUntil]?.toString(), eventId=row[OrderItemsTable.eventId].toString(), sectionId=row[OrderItemsTable.sectionId]?.toString(), seatId=row[OrderItemsTable.seatId]?.toString(), unitPrice=row[OrderItemsTable.unitPrice].toPlainString(), currency=row[OrderItemsTable.currency], status=row[OrderItemsTable.status], refundedAmount=row[OrderItemsTable.refundedAmount].toPlainString(), discountAmount=row[OrderItemsTable.discountAmount].toPlainString())}
    private fun toResponse(row:ResultRow):OrderResponse{val its=items(row[OrdersTable.id]);return OrderResponse(id=row[OrdersTable.id].toString(), userId=row[OrdersTable.userId]?.toString(), inventoryId=its.singleOrNull()?.inventoryId, reservationId=its.singleOrNull()?.reservationId, paymentId=row[OrdersTable.paymentId]?.toString(), amount=row[OrdersTable.amount].toPlainString(), currency=row[OrdersTable.currency], discountAmount=row[OrdersTable.discountAmount].toPlainString(), promotionCode=row[OrdersTable.promotionCode], status=OrderStatus.valueOf(row[OrdersTable.status]), failureReason=row[OrdersTable.failureReason], items=its, reservedUntil=its.mapNotNull { it.reservedUntil }.minOrNull(), createdAt=row[OrdersTable.createdAt].toString(), updatedAt=row[OrdersTable.updatedAt].toString())}
    fun refundTicket(ticketId: UUID): IssuedTicketResponse = transaction {
        val ticket = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.id eq ticketId }.singleOrNull()
            ?: error("Ticket not found")
        require(ticket[IssuedTicketsTable.status] == IssuedTicketStatus.ISSUED.name) { "Only ISSUED tickets can be refunded" }
        val itemId = ticket[IssuedTicketsTable.orderItemId]
        val item = OrderItemsTable.selectAll().where { OrderItemsTable.id eq itemId }.single()
        require(item[OrderItemsTable.status] == "ACTIVE") { "Ticket item is already refunded" }
        OrderItemsTable.update({ OrderItemsTable.id eq itemId }) {
            it[status] = "REFUNDED"; it[refundedAmount] = item[OrderItemsTable.unitPrice].subtract(item[OrderItemsTable.discountAmount])
        }
        IssuedTicketsTable.update({ IssuedTicketsTable.id eq ticketId }) { it[status] = IssuedTicketStatus.CANCELLED.name }
        findTicketByIdInternal(ticketId)!!
    }

    fun transferTicket(ticketId: UUID, fromUserId: UUID, recipientEmail: String): TicketTransferResponse = transaction {
        val ticket = IssuedTicketsTable.selectAll().where { IssuedTicketsTable.id eq ticketId }.singleOrNull() ?: error("Ticket not found")
        require(ticket[IssuedTicketsTable.userId] == fromUserId) { "Ticket does not belong to authenticated user" }
        require(ticket[IssuedTicketsTable.status] == IssuedTicketStatus.ISSUED.name) { "Only ISSUED tickets can be transferred" }
        val pending = TicketTransfersTable.selectAll().where { (TicketTransfersTable.ticketId eq ticketId) and (TicketTransfersTable.status eq "PENDING") }.any()
        require(!pending) { "Ticket already has a pending transfer" }
        val id=UUID.randomUUID(); val token=UUID.randomUUID(); val now=OffsetDateTime.now(ZoneOffset.UTC); val expires=now.plusDays(7)
        TicketTransfersTable.insert { it[TicketTransfersTable.id]=id;it[TicketTransfersTable.ticketId]=ticketId;it[TicketTransfersTable.fromUserId]=fromUserId;it[TicketTransfersTable.recipientEmail]=recipientEmail.lowercase();it[status]="PENDING";it[transferToken]=token;it[expiresAt]=expires;it[createdAt]=now }
        findTransferInternal(id)!!
    }

    fun acceptTransfer(token: UUID, recipientUserId: UUID, recipientEmail: String): TicketTransferResponse = transaction {
        val row=TicketTransfersTable.selectAll().where { TicketTransfersTable.transferToken eq token }.singleOrNull() ?: error("Transfer not found")
        require(row[TicketTransfersTable.status]=="PENDING") { "Transfer is not pending" }
        require(row[TicketTransfersTable.expiresAt].isAfter(OffsetDateTime.now(ZoneOffset.UTC))) { "Transfer has expired" }
        require(row[TicketTransfersTable.recipientEmail].equals(recipientEmail, true)) { "Transfer was sent to a different email" }
        val ticketId=row[TicketTransfersTable.ticketId]; val from=row[TicketTransfersTable.fromUserId]; val now=OffsetDateTime.now(ZoneOffset.UTC)
        IssuedTicketsTable.update({ IssuedTicketsTable.id eq ticketId }) { it[userId]=recipientUserId }
        TicketTransfersTable.update({ TicketTransfersTable.id eq row[TicketTransfersTable.id] }) { it[status]="ACCEPTED";it[acceptedByUserId]=recipientUserId;it[acceptedAt]=now }
        TicketOwnershipHistoryTable.insert { it[id]=UUID.randomUUID();it[TicketOwnershipHistoryTable.ticketId]=ticketId;it[fromUserId]=from;it[toUserId]=recipientUserId;it[reason]="TRANSFER";it[transferId]=row[TicketTransfersTable.id];it[createdAt]=now }
        findTransferInternal(row[TicketTransfersTable.id])!!
    }

    fun findTransfersFor(email: String, userId: UUID): List<TicketTransferResponse> = transaction {
        TicketTransfersTable.selectAll().where { (TicketTransfersTable.recipientEmail eq email.lowercase()) or (TicketTransfersTable.fromUserId eq userId) }.orderBy(TicketTransfersTable.createdAt to SortOrder.DESC).map(::toTransfer)
    }

    fun analytics(eventId: UUID): EventSalesAnalyticsResponse = transaction {
        val tickets=IssuedTicketsTable.selectAll().where { IssuedTicketsTable.eventId eq eventId }.toList()
        val itemIds=tickets.map { it[IssuedTicketsTable.orderItemId] }
        val items=if(itemIds.isEmpty()) emptyList() else OrderItemsTable.selectAll().where { OrderItemsTable.id inList itemIds }.toList()
        val gross=items.sumOf { it[OrderItemsTable.unitPrice] }
        val refunded=items.sumOf { it[OrderItemsTable.refundedAmount] }
        val orderCount=tickets.map { it[IssuedTicketsTable.orderId] }.distinct().size
        val groups=tickets.groupBy { it[IssuedTicketsTable.sectionId] }
        val bySection=groups.map { (section, rows) ->
            val ids=rows.map { it[IssuedTicketsTable.orderItemId] }.toSet(); val sectionItems=items.filter { it[OrderItemsTable.id] in ids }
            SectionSalesAnalytics(section?.toString(), rows.count { it[IssuedTicketsTable.status] != IssuedTicketStatus.CANCELLED.name }, rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.CANCELLED.name }, rows.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.USED.name }, sectionItems.sumOf { it[OrderItemsTable.unitPrice] }.toPlainString())
        }
        EventSalesAnalyticsResponse(eventId.toString(),orderCount,tickets.count { it[IssuedTicketsTable.status] != IssuedTicketStatus.CANCELLED.name },tickets.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.CANCELLED.name },tickets.count { it[IssuedTicketsTable.status] == IssuedTicketStatus.USED.name },gross.toPlainString(),refunded.toPlainString(),gross.subtract(refunded).toPlainString(),0.0,bySection)
    }

    private fun findTicketByIdInternal(ticketId:UUID)=IssuedTicketsTable.selectAll().where { IssuedTicketsTable.id eq ticketId }.singleOrNull()?.let(::toTicket)
    private fun findTransferInternal(id:UUID)=TicketTransfersTable.selectAll().where { TicketTransfersTable.id eq id }.singleOrNull()?.let(::toTransfer)
    private fun toTransfer(r:ResultRow)=TicketTransferResponse(r[TicketTransfersTable.id].toString(),r[TicketTransfersTable.ticketId].toString(),r[TicketTransfersTable.fromUserId].toString(),r[TicketTransfersTable.recipientEmail],r[TicketTransfersTable.status],r[TicketTransfersTable.transferToken].toString(),r[TicketTransfersTable.expiresAt].toString(),r[TicketTransfersTable.acceptedByUserId]?.toString(),r[TicketTransfersTable.createdAt].toString(),r[TicketTransfersTable.acceptedAt]?.toString())

    fun createPromotion(r:CreatePromotionRequest):PromotionResponse=transaction{
        require(!PromotionsTable.selectAll().where{PromotionsTable.code eq r.code.trim().uppercase()}.any()){ "Promotion code already exists" };val id=UUID.randomUUID();val now=OffsetDateTime.now(ZoneOffset.UTC);PromotionsTable.insert{it[PromotionsTable.id]=id;it[code]=r.code.trim().uppercase();it[description]=r.description;it[discountType]=r.discountType.uppercase();it[discountValue]=r.discountValue.toBigDecimal();it[eventId]=r.eventId?.let(UUID::fromString);it[startsAt]=r.startsAt?.let(OffsetDateTime::parse);it[endsAt]=r.endsAt?.let(OffsetDateTime::parse);it[maxUses]=r.maxUses;it[maxUsesPerUser]=r.maxUsesPerUser;it[currentUses]=0;it[active]=r.active;it[createdAt]=now;it[updatedAt]=now};findPromotionInternal(id)!!
    }
    fun listPromotions(): List<PromotionResponse> = transaction {
        PromotionsTable
            .selectAll()
            .orderBy(PromotionsTable.createdAt to SortOrder.DESC)
            .map(::toPromotion)
    }
    fun applyPromotion(orderId:UUID,userId:UUID,code:String):OrderResponse=transaction{
        val order=findByIdInternal(orderId)?:error("Order not found");require(order.userId==userId.toString()){ "Order does not belong to user" };require(order.status==OrderStatus.RESERVED){ "Promotion can only be applied before checkout" };require(order.promotionCode==null){ "Order already has a promotion" }
        val now=OffsetDateTime.now(ZoneOffset.UTC);val row=PromotionsTable.selectAll().where{PromotionsTable.code eq code.trim().uppercase()}.singleOrNull()?:error("Promotion not found");require(row[PromotionsTable.active]){"Promotion is inactive"};require(row[PromotionsTable.startsAt]?.let{!it.isAfter(now)}?:true){"Promotion has not started"};require(row[PromotionsTable.endsAt]?.let{it.isAfter(now)}?:true){"Promotion has expired"};require(row[PromotionsTable.maxUses]?.let{row[PromotionsTable.currentUses]<it}?:true){"Promotion usage limit reached"};val event=row[PromotionsTable.eventId];require(event==null||order.items.all{it.eventId==event.toString()}){"Promotion does not apply to this event"};val used=PromotionRedemptionsTable.selectAll().where{(PromotionRedemptionsTable.promotionId eq row[PromotionsTable.id]) and (PromotionRedemptionsTable.userId eq userId)}.count().toInt();require(used<row[PromotionsTable.maxUsesPerUser]){"Promotion usage limit reached for user"}
        val gross=BigDecimal(order.amount);val raw=if(row[PromotionsTable.discountType]=="PERCENTAGE")gross.multiply(row[PromotionsTable.discountValue]).divide(BigDecimal("100")) else row[PromotionsTable.discountValue];val discount=raw.min(gross).setScale(2);val net=gross.subtract(discount)
        OrdersTable.update({OrdersTable.id eq orderId}){it[amount]=net;it[discountAmount]=discount;it[promotionCode]=row[PromotionsTable.code];it[updatedAt]=now};val orderRows=OrderItemsTable.selectAll().where{OrderItemsTable.orderId eq orderId}.toList();var allocated=BigDecimal.ZERO;orderRows.forEachIndexed{index,item->val share=if(index==orderRows.lastIndex)discount.subtract(allocated) else discount.multiply(item[OrderItemsTable.unitPrice]).divide(gross,2,java.math.RoundingMode.HALF_UP);allocated=allocated.add(share);OrderItemsTable.update({OrderItemsTable.id eq item[OrderItemsTable.id]}){it[discountAmount]=share}};PromotionRedemptionsTable.insert{it[id]=UUID.randomUUID();it[promotionId]=row[PromotionsTable.id];it[PromotionRedemptionsTable.orderId]=orderId;it[PromotionRedemptionsTable.userId]=userId;it[discountAmount]=discount;it[createdAt]=now};PromotionsTable.update({PromotionsTable.id eq row[PromotionsTable.id]}){it[currentUses]=row[PromotionsTable.currentUses]+1;it[updatedAt]=now};findByIdInternal(orderId)!!
    }
    private fun findPromotionInternal(id:UUID)=PromotionsTable.selectAll().where{PromotionsTable.id eq id}.singleOrNull()?.let(::toPromotion)
    private fun toPromotion(r:ResultRow)=PromotionResponse(r[PromotionsTable.id].toString(),r[PromotionsTable.code],r[PromotionsTable.description],r[PromotionsTable.discountType],r[PromotionsTable.discountValue].toPlainString(),r[PromotionsTable.eventId]?.toString(),r[PromotionsTable.startsAt]?.toString(),r[PromotionsTable.endsAt]?.toString(),r[PromotionsTable.maxUses],r[PromotionsTable.maxUsesPerUser],r[PromotionsTable.currentUses],r[PromotionsTable.active])

    fun refundActiveOrderItems(orderId:UUID)=transaction{val rows=OrderItemsTable.selectAll().where{(OrderItemsTable.orderId eq orderId) and (OrderItemsTable.status eq "ACTIVE")}.toList();rows.forEach{row->OrderItemsTable.update({OrderItemsTable.id eq row[OrderItemsTable.id]}){it[status]="REFUNDED";it[refundedAmount]=row[OrderItemsTable.unitPrice].subtract(row[OrderItemsTable.discountAmount])}}}

}
