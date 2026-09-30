package com.ticketflow.orders.modules.orders

import com.ticketflow.orders.clients.payments.*
import com.ticketflow.orders.clients.tickets.*
import java.util.UUID

class OrderService(private val repository:OrderRepository,private val ticketsClient:TicketsClient,private val paymentsClient:PaymentsClient){
 suspend fun create(userId:UUID,request:CreateOrderRequest):OrderResponse{
  val raw=(request.inventoryIds + listOfNotNull(request.inventoryId)).distinct()
  require(raw.isNotEmpty()){ "At least one inventoryId is required" };require(raw.size<=10){"An order can contain at most 10 tickets"}
  val inventory=raw.map{ id-> val uuid=parseUuid(id,"Invalid inventoryId");ticketsClient.findInventoryById(uuid.toString())?:throw TicketNotAvailableException() }
  require(inventory.map{it.eventId}.distinct().size==1){"All tickets in an order must belong to the same event"}
  if(inventory.any{it.status!=TicketInventoryStatus.AVAILABLE})throw TicketNotAvailableException()
  val order=repository.createPending(userId,inventory);val orderId=UUID.fromString(order.id);val reserved=mutableListOf<Pair<UUID,UUID>>()
  try{
   inventory.forEach{item->val iid=UUID.fromString(item.id);val response=ticketsClient.reserve(item.id)?:throw TicketNotAvailableException();val rid=response.inventory.reservationId?.let(UUID::fromString)?:error("Tickets Service returned no reservationId");repository.attachReservation(orderId,iid,rid);reserved+=iid to rid}
  }catch(cause:Exception){reserved.forEach{(iid,rid)->compensateReservation(iid,rid)};repository.markFailed(orderId,"Unable to reserve all tickets");throw cause}
  return repository.markReserved(orderId)?:repository.markFailed(orderId,"Unable to persist reservation")
 }
 suspend fun confirm(userId:UUID,orderId:UUID,userEmail:String,isAdmin:Boolean=false):OrderResponse{
  val order=findById(orderId);ensureOwner(order,userId,isAdmin);if(order.status==OrderStatus.CONFIRMED)return order
  if(order.status!=OrderStatus.RESERVED)throw InvalidOrderStateException("Only RESERVED orders can be confirmed")
  val payment=paymentsClient.create(CreatePaymentRequest(order.id,userId.toString(),order.amount,order.currency,"order-confirm-${order.id}"))
  if(payment.status!=PaymentStatus.SUCCEEDED)throw OrderOperationException("Payment was not successful")
  repository.attachPayment(orderId,UUID.fromString(payment.id))?:throw OrderOperationException("Unable to attach payment to order")
  order.items.forEach{item->val rid=item.reservationId?:throw OrderOperationException("Order item has no reservationId");val inv=ticketsClient.confirm(item.inventoryId,rid)?:throw OrderOperationException("Ticket reservation could not be confirmed");if(inv.status!=TicketInventoryStatus.SOLD)throw OrderOperationException("Tickets Service did not mark inventory as SOLD")}
  val confirmed=repository.markConfirmed(orderId,userEmail)?:throw OrderOperationException("Unable to mark order as CONFIRMED");repository.issueTickets(orderId,userId);return confirmed
 }
 suspend fun cancel(userId:UUID,orderId:UUID,isAdmin:Boolean=false):OrderResponse{
  val order=findById(orderId);ensureOwner(order,userId,isAdmin);if(order.status==OrderStatus.CANCELLED)return order
  if(order.status==OrderStatus.CONFIRMED){val pid=order.paymentId?:throw OrderOperationException("Confirmed order has no paymentId");val refund=paymentsClient.refund(pid);if(refund.status!=PaymentStatus.REFUNDED)throw OrderOperationException("Payment could not be refunded");order.items.forEach{item->ticketsClient.restock(item.inventoryId)?:throw OrderOperationException("Refund succeeded but inventory could not be restocked")};repository.cancelIssuedTickets(orderId);return repository.markCancelled(orderId)?:throw OrderOperationException("Unable to cancel order")}
  if(order.status!=OrderStatus.RESERVED)throw InvalidOrderStateException("Only RESERVED or CONFIRMED orders can be cancelled")
  order.items.forEach{item->val rid=item.reservationId?:return@forEach;val inv=ticketsClient.release(item.inventoryId,rid)?:throw OrderOperationException("Ticket reservation could not be released");if(inv.status!=TicketInventoryStatus.AVAILABLE)throw OrderOperationException("Tickets Service did not release inventory")}
  return repository.markCancelled(orderId)?:throw OrderOperationException("Unable to cancel order")
 }
 fun findById(id:UUID)=repository.findById(id)?:throw OrderNotFoundException(id.toString())
 fun findForUser(userId:UUID,isAdmin:Boolean)=if(isAdmin)repository.findAll() else repository.findByUser(userId)
 fun findForUser(orderId:UUID,userId:UUID,isAdmin:Boolean):OrderResponse{val o=findById(orderId);ensureOwner(o,userId,isAdmin);return o}
 fun findTickets(userId:UUID)=repository.findTicketsByUser(userId)
 fun checkIn(token:UUID,isAdmin:Boolean):IssuedTicketResponse{if(!isAdmin)throw SecurityException("ADMIN role required");val ticket=repository.checkIn(token)?:throw OrderOperationException("Ticket not found");if(ticket.status!=IssuedTicketStatus.USED)throw OrderOperationException("Ticket is not valid for check-in");return ticket}
 private fun ensureOwner(o:OrderResponse,u:UUID,a:Boolean){if(!a&&o.userId!=u.toString())throw SecurityException("Order does not belong to authenticated user")}
 private suspend fun compensateReservation(i:UUID,r:UUID){try{ticketsClient.release(i.toString(),r.toString())}catch(_:Exception){}}
 private fun parseUuid(v:String,m:String)=try{UUID.fromString(v)}catch(_:IllegalArgumentException){throw IllegalArgumentException(m)}
}
