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
            event.status ==
                    EventStatus.PUBLISHED,
        ) {
            "Ticket inventory can only be created for published events"
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
}