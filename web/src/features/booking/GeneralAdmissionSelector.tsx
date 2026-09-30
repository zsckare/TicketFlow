import { formatMoney } from '../../lib/format'
import type { TicketInventoryResponse } from '../../types/tickets'

interface Props {
  sectionName: string
  available: TicketInventoryResponse[]
  selectedCount: number
  disabled?: boolean
  onAdd: () => void
  onRemove: () => void
}

export function GeneralAdmissionSelector({ sectionName, available, selectedCount, disabled = false, onAdd, onRemove }: Props) {
  const sample = available[0]
  return <div className="ga-selector">
    <div className="ga-icon" aria-hidden="true">GA</div>
    <div className="ga-copy">
      <span className="eyebrow">Admisión general</span>
      <h3>{sectionName}</h3>
      <p>Acceso sin asiento asignado. Llega con tiempo para elegir tu lugar dentro de la sección.</p>
      <div className="availability-line"><span className="status-dot" />{available.length} boletos disponibles</div>
    </div>
    <div className="ga-purchase">
      {sample && <strong>{formatMoney(sample.price, sample.currency)} <small>c/u</small></strong>}
      <div className="quantity-stepper" aria-label="Cantidad de boletos">
        <button type="button" onClick={onRemove} disabled={disabled || selectedCount === 0} aria-label="Quitar boleto">−</button>
        <span>{selectedCount}</span>
        <button type="button" onClick={onAdd} disabled={disabled || selectedCount >= available.length} aria-label="Agregar boleto">+</button>
      </div>
    </div>
  </div>
}
