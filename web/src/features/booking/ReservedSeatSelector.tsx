import type { SeatResponse } from '../../types/events'
import type { TicketInventoryResponse } from '../../types/tickets'

interface Props {
  seats: SeatResponse[]
  inventoryBySeat: Map<string, TicketInventoryResponse>
  selectedIds: Set<string>
  disabled?: boolean
  onToggle: (inventory: TicketInventoryResponse, seat: SeatResponse) => void
}

export function ReservedSeatSelector({ seats, inventoryBySeat, selectedIds, disabled = false, onToggle }: Props) {
  return <div className="seat-selector">
    <div className="stage"><span>ESCENARIO</span></div>
    <div className="seat-legend">
      <span><i className="legend-dot available-dot" />Disponible</span>
      <span><i className="legend-dot selected-dot" />Seleccionado</span>
      <span><i className="legend-dot unavailable-dot" />No disponible</span>
    </div>
    <div className={`seat-grid ${seats.some(s=>s.mapX != null && s.mapY != null) ? 'seat-grid-mapped' : ''}`}>
      {seats.map(seat => {
        const inventory = inventoryBySeat.get(seat.id)
        const available = inventory?.status === 'AVAILABLE'
        const selected = inventory ? selectedIds.has(inventory.id) : false
        return <button
          type="button"
          key={seat.id}
          style={seat.mapX != null && seat.mapY != null ? { left: `${seat.mapX}%`, top: `${seat.mapY}%` } : undefined}
          className={`seat ${available ? 'available' : 'unavailable'} ${selected ? 'selected' : ''}`}
          disabled={!available || disabled}
          aria-pressed={selected}
          onClick={() => inventory && onToggle(inventory, seat)}
        >
          <b>{seat.row}{seat.number}</b>
          <small>{selected ? 'SELECCIONADO' : available ? 'DISPONIBLE' : inventory?.status ?? 'SIN BOLETO'}</small>
        </button>
      })}
    </div>
  </div>
}
