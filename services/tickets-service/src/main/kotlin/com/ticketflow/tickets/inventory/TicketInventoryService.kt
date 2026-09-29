package com.ticketflow.tickets.modules.inventory

import com.ticketflow.tickets.clients.events.EventStatus
import com.ticketflow.tickets.clients.events.EventsClient
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Reglas de negocio del inventario.
 */
class TicketInventoryService(
    private val repository: TicketInventoryRepository,
    private val eventsClient: EventsClient,
) {

    /**
     * Tiempo durante el cual un comprador puede mantener
     * temporalmente reservado un ticket.
     */
    private val reservationDurationMinutes =
        10L

    /**
     * Crea inventario para un asiento dentro de un evento.
     */
    suspend fun create(
        request: CreateTicketInventoryRequest,
    ): TicketInventoryResponse {

        val eventId =
            runCatching {
                UUID.fromString(
                    request.eventId,
                )
            }.getOrElse {
                throw IllegalArgumentException(
                    "Invalid event ID",
                )
            }

        val seatId =
            runCatching {
                UUID.fromString(
                    request.seatId,
                )
            }.getOrElse {
                throw IllegalArgumentException(
                    "Invalid seat ID",
                )
            }

        /*
         * Events Service es propietario del Event.
         *
         * Tickets Service no accede directamente a events_db.
         */
        val event =
            eventsClient.findEventById(
                eventId.toString(),
            ) ?: throw IllegalArgumentException(
                "Event does not exist",
            )

        require(
            event.status == EventStatus.DRAFT ||
                    event.status == EventStatus.PUBLISHED,
        ) {
            "Ticket inventory can only be created for draft or published events"
        }

        /*
         * Comprobamos que el asiento realmente existe.
         */
        val seat =
            eventsClient.findSeatById(
                seatId.toString(),
            ) ?: throw IllegalArgumentException(
                "Seat does not exist",
            )

        /*
         * Obtenemos la sección propietaria del asiento.
         */
        val section =
            eventsClient.findSectionById(
                seat.sectionId,
            ) ?: throw IllegalArgumentException(
                "Seat section does not exist",
            )

        /*
         * La sección del asiento debe pertenecer al mismo
         * venue en el que se realizará el evento.
         */
        require(
            section.venueId ==
                    event.venueId,
        ) {
            "Seat does not belong to the event venue"
        }

        val price =
            request.price
                .toBigDecimalOrNull()
                ?: throw IllegalArgumentException(
                    "Invalid price",
                )

        require(
            price >= BigDecimal.ZERO,
        ) {
            "Price cannot be negative"
        }

        val currency =
            request.currency
                .trim()
                .uppercase()

        require(
            currency.length == 3 &&
                    currency.all {
                        it.isLetter()
                    },
        ) {
            "Currency must be a 3-letter code"
        }

        return repository.create(
            eventId = eventId,
            seatId = seatId,
            price = price,
            currency = currency,
        )
    }

    /**
     * Obtiene un registro de inventario.
     */
    fun findById(
        inventoryId: UUID,
    ): TicketInventoryResponse? =
        repository.findById(
            inventoryId,
        )

    /**
     * Obtiene todo el inventario perteneciente
     * a un evento.
     */
    fun findByEvent(
        eventId: UUID,
    ): List<TicketInventoryResponse> =
        repository.findByEvent(
            eventId,
        )

    /**
     * Intenta reservar temporalmente un ticket.
     *
     * La garantía real contra concurrencia se encuentra
     * dentro de repository.reserve(), donde PostgreSQL hace:
     *
     * UPDATE ... WHERE status = AVAILABLE
     */
    fun reserve(
        inventoryId: UUID,
    ): ReserveTicketResponse {

        /*
         * Antes de reservar liberamos cualquier reservación
         * que ya haya expirado.
         *
         * Esta solución es suficiente por ahora.
         * Más adelante moveremos expiraciones a un mecanismo
         * especializado.
         */
        releaseExpired()

        val existing =
            repository.findById(
                inventoryId,
            )
                ?: throw TicketInventoryNotFoundException(
                    inventoryId.toString(),
                )

        /*
         * Esta comprobación produce un error más claro.
         *
         * NO es nuestra protección contra concurrencia.
         * La protección real continúa siendo el UPDATE
         * condicional del repository.
         */
        if (
            existing.status !=
            TicketInventoryStatus.AVAILABLE
        ) {
            throw TicketInventoryConflictException(
                "Ticket is not available",
            )
        }

        val reservationId =
            UUID.randomUUID()

        val reservedUntil =
            OffsetDateTime
                .now(
                    ZoneOffset.UTC,
                )
                .plusMinutes(
                    reservationDurationMinutes,
                )

        val inventory =
            repository.reserve(
                inventoryId = inventoryId,
                reservationId = reservationId,
                reservedUntil = reservedUntil,
            )
                ?: throw TicketInventoryConflictException(
                    "Ticket is not available",
                )

        return ReserveTicketResponse(
            inventory = inventory,
        )
    }

    /**
     * Libera explícitamente una reservación.
     *
     * Solamente el reservationId propietario puede
     * realizar esta operación.
     */
    fun release(
        inventoryId: UUID,
        reservationId: UUID,
    ): TicketInventoryResponse {

        val existing =
            repository.findById(
                inventoryId,
            )
                ?: throw TicketInventoryNotFoundException(
                    inventoryId.toString(),
                )

        if (
            existing.status !=
            TicketInventoryStatus.RESERVED
        ) {
            throw TicketInventoryConflictException(
                "Ticket is not reserved",
            )
        }

        return repository.release(
            inventoryId = inventoryId,
            reservationId = reservationId,
        )
            ?: throw InvalidReservationException(
                "Reservation does not own this ticket",
            )
    }

    /**
     * Convierte una reservación válida en SOLD.
     *
     * Por ahora esta operación vive directamente en Tickets.
     * Posteriormente será coordinada por Orders/Payments.
     */
    fun confirm(
        inventoryId: UUID,
        reservationId: UUID,
    ): TicketInventoryResponse {

        /*
         * Una reservación expirada nunca debe poder
         * convertirse en una venta.
         */
        releaseExpired()

        val existing =
            repository.findById(
                inventoryId,
            )
                ?: throw TicketInventoryNotFoundException(
                    inventoryId.toString(),
                )

        if (
            existing.status !=
            TicketInventoryStatus.RESERVED
        ) {
            throw TicketInventoryConflictException(
                "Ticket is not reserved",
            )
        }

        val now =
            OffsetDateTime.now(
                ZoneOffset.UTC,
            )

        return repository.confirm(
            inventoryId = inventoryId,
            reservationId = reservationId,
            now = now,
        )
            ?: throw InvalidReservationException(
                "Reservation is invalid or expired",
            )
    }

    /**
     * Libera todas las reservaciones cuyo TTL ya expiró.
     *
     * Retorna la cantidad de tickets liberados.
     */
    fun releaseExpired(): Int {

        val now =
            OffsetDateTime.now(
                ZoneOffset.UTC,
            )

        return repository.releaseExpired(
            now,
        )
    }

    /** Configures a complete event section and generates its sellable units. */
    suspend fun configureSection(eventIdValue: String, request: ConfigureSectionInventoryRequest): EventSectionInventoryResponse {
        val eventId = parseId(eventIdValue, "event")
        val sectionId = parseId(request.sectionId, "section")
        val event = eventsClient.findEventById(eventId.toString()) ?: throw IllegalArgumentException("Event does not exist")
        require(event.status == EventStatus.DRAFT) { "Section inventory can only be configured while the event is DRAFT" }
        val section = eventsClient.findSectionById(sectionId.toString()) ?: throw IllegalArgumentException("Section does not exist")
        require(section.venueId == event.venueId) { "Section does not belong to the event venue" }
        val basePrice = request.basePrice.toBigDecimalOrNull() ?: throw IllegalArgumentException("Invalid base price")
        require(basePrice >= BigDecimal.ZERO) { "Base price cannot be negative" }
        val currency = request.currency.trim().uppercase()
        require(currency.length == 3 && currency.all(Char::isLetter)) { "Currency must be a 3-letter code" }

        val type = InventorySectionType.valueOf(section.type.name)
        val seats = if (type == InventorySectionType.RESERVED_SEATING) {
            eventsClient.findSeatsBySection(sectionId.toString())
        } else emptyList()
        if (type == InventorySectionType.RESERVED_SEATING) {
            require(seats.isNotEmpty()) { "Reserved seating section has no seats" }
        }
        val capacity = if (type == InventorySectionType.GENERAL_ADMISSION) {
            request.quantity ?: section.capacity
        } else seats.size
        require(capacity in 1..section.capacity) { "Quantity must be between 1 and the section capacity" }

        return repository.configureSection(
            eventId, sectionId, type, basePrice, currency, capacity,
            seats.map { UUID.fromString(it.id) },
        )
    }

    fun findSectionConfigs(eventId: UUID): List<EventSectionInventoryResponse> =
        repository.findSectionConfigs(eventId)

    suspend fun updateSeatPrice(eventIdValue: String, seatIdValue: String, request: UpdateSeatPriceRequest): TicketInventoryResponse {
        val eventId = parseId(eventIdValue, "event")
        val seatId = parseId(seatIdValue, "seat")
        val event = eventsClient.findEventById(eventId.toString()) ?: throw IllegalArgumentException("Event does not exist")
        require(event.status == EventStatus.DRAFT) { "Seat prices can only be changed while the event is DRAFT" }
        val override = request.priceOverride?.let {
            it.toBigDecimalOrNull() ?: throw IllegalArgumentException("Invalid price override")
        }
        require(override == null || override >= BigDecimal.ZERO) { "Price override cannot be negative" }
        return repository.updateSeatOverride(eventId, seatId, override)
            ?: throw IllegalArgumentException("Seat inventory does not exist for this event")
    }

    private fun parseId(value: String, name: String): UUID =
        runCatching { UUID.fromString(value) }.getOrElse { throw IllegalArgumentException("Invalid $name ID") }

}