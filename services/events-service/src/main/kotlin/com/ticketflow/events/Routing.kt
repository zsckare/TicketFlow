package com.ticketflow.events

import com.ticketflow.events.modules.events.EventRepository
import com.ticketflow.events.modules.events.EventService
import com.ticketflow.events.modules.events.eventRoutes
import com.ticketflow.events.modules.seats.SeatRepository
import com.ticketflow.events.modules.seats.SeatService
import com.ticketflow.events.modules.seats.seatRoutes
import com.ticketflow.events.modules.sections.VenueSectionRepository
import com.ticketflow.events.modules.sections.VenueSectionService
import com.ticketflow.events.modules.sections.venueSectionRoutes
import com.ticketflow.events.modules.venues.VenueRepository
import com.ticketflow.events.modules.venues.VenueService
import com.ticketflow.events.modules.venues.venueRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    /*
     * Venue
     */
    val venueRepository =
        VenueRepository()

    val venueService =
        VenueService(
            repository = venueRepository,
        )

    /*
     * Venue Sections
     */
    val sectionRepository =
        VenueSectionRepository()

    val sectionService =
        VenueSectionService(
            venueRepository = venueRepository,
            repository = sectionRepository,
        )

    /*
     * Events
     */
    val eventRepository =
        EventRepository()

    val eventService =
        EventService(
            eventRepository = eventRepository,
            venueRepository = venueRepository,
        )

    /*
 * Seats
 */
    val seatRepository =
        SeatRepository()

    val seatService =
        SeatService(
            sectionRepository = sectionRepository,
            repository = seatRepository,
        )
    routing {

        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "events-service",
                    "status" to "UP",
                ),
            )
        }

        venueRoutes(
            service = venueService,
        )

        venueSectionRoutes(
            service = sectionService,
        )

        eventRoutes(
            service = eventService,
        )
        seatRoutes(
            service = seatService,
        )
    }
}