package com.ticketflow.events.modules.seats

import com.ticketflow.events.modules.sections.VenueSectionRepository
import com.ticketflow.events.modules.sections.VenueSectionType
import java.util.UUID

/**
 * Reglas de negocio relacionadas con los asientos físicos.
 */
class SeatService(
    private val sectionRepository: VenueSectionRepository,
    private val repository: SeatRepository,
) {

    /**
     * Crea un asiento.
     *
     * Solamente las secciones RESERVED_SEATING pueden
     * contener asientos individuales.
     */
    fun create(
        sectionId: UUID,
        request: CreateSeatRequest,
    ): SeatResponse {

        val section =
            sectionRepository.findById(sectionId)
                ?: throw IllegalArgumentException(
                    "Section does not exist",
                )

        require(
            section.type == VenueSectionType.RESERVED_SEATING,
        ) {
            "Seats can only be created in RESERVED_SEATING sections"
        }

        require(
            request.row.isNotBlank(),
        ) {
            "Seat row cannot be blank"
        }

        require(
            request.number.isNotBlank(),
        ) {
            "Seat number cannot be blank"
        }

        return repository.create(
            sectionId = sectionId,
            row = request.row.trim(),
            number = request.number.trim(),
        )
    }

    /**
     * Obtiene los asientos de una sección.
     */
    fun findBySection(
        sectionId: UUID,
    ): List<SeatResponse> {

        val section =
            sectionRepository.findById(sectionId)
                ?: throw IllegalArgumentException(
                    "Section does not exist",
                )

        /**
         * Una GENERAL_ADMISSION simplemente regresará una
         * colección vacía porque no utiliza asientos físicos.
         */
        if (
            section.type ==
            VenueSectionType.GENERAL_ADMISSION
        ) {
            return emptyList()
        }

        return repository.findBySection(
            sectionId,
        )
    }

    fun findById(
        id: UUID,
    ): SeatResponse? =
        repository.findById(id)
    fun updateMap(id:UUID,request:UpdateSeatMapRequest)=repository.updateMap(id,request)

}