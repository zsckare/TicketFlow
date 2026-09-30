import { formatMoney } from '../../lib/format'
import type { OrderResponse } from '../../types/orders'
import type { BookingSelection } from './types'
import { ReservationTimer } from './ReservationTimer'

interface Props {
  selections: BookingSelection[]
  order?: OrderResponse
  busy?: boolean
  onRemove: (inventoryId: string) => void
  onContinue: () => void
  onConfirm: () => void
  onCancel: () => void
}

export function BookingCart({ selections, order, busy = false, onRemove, onContinue, onConfirm, onCancel }: Props) {
  const total = selections.reduce((sum, item) => sum + Number(item.inventory.price), 0)
  const currency = selections[0]?.inventory.currency ?? order?.currency ?? 'MXN'
  const gaGroups = Array.from(new Set(selections.filter(x => x.sectionType === 'GENERAL_ADMISSION').map(x => x.inventory.sectionId)))
    .map(sectionId => {
      const items = selections.filter(x => x.sectionType === 'GENERAL_ADMISSION' && x.inventory.sectionId === sectionId)
      return { sectionId, sectionName: items[0]?.sectionName ?? 'Admisión general', items }
    })
  const reserved = selections.filter(x => x.sectionType === 'RESERVED_SEATING')
  const orderItems = order?.items ?? []

  return <aside className="panel checkout booking-cart">
    <div className="cart-heading">
      <div><span className="eyebrow">Tu selección</span><h2>{order?.status === 'RESERVED' ? 'Reserva activa' : 'Resumen de compra'}</h2></div>
      {!!(selections.length || orderItems.length) && <span className="cart-count">{selections.length || orderItems.length}</span>}
    </div>

    {!selections.length && !order && <div className="cart-empty"><div className="cart-empty-icon">＋</div><b>Tu selección está vacía</b><p>Elige uno o más boletos para ver aquí el resumen de tu compra.</p></div>}

    {!!reserved.length && <div className="cart-group"><span className="cart-group-label">Asientos numerados</span>{reserved.map(item => <div className="cart-line" key={item.inventory.id}><div><b>{item.seatLabel ?? 'Asiento numerado'}</b><small>{item.sectionName}</small></div><div className="cart-line-end"><strong>{formatMoney(item.inventory.price, item.inventory.currency)}</strong>{!order && <button className="icon-button" onClick={() => onRemove(item.inventory.id)} aria-label={`Quitar ${item.seatLabel ?? 'boleto'}`}>×</button>}</div></div>)}</div>}

    {gaGroups.map(group => { const subtotal = group.items.reduce((sum, item) => sum + Number(item.inventory.price), 0); const first = group.items[0]; return <div className="cart-group" key={group.sectionId ?? group.sectionName}><span className="cart-group-label">Admisión general</span><div className="cart-line"><div><b>{group.sectionName} × {group.items.length}</b><small>{first ? `${formatMoney(first.inventory.price, first.inventory.currency)} c/u` : ''}</small></div><div className="cart-line-end"><strong>{formatMoney(String(subtotal), currency)}</strong>{!order && <button className="icon-button" onClick={() => first && onRemove(first.inventory.id)} aria-label="Quitar un boleto general">−</button>}</div></div></div> })}

    {!!order && !selections.length && <div className="cart-group"><span className="cart-group-label">Boletos reservados</span>{orderItems.map(item => <div className="cart-line" key={item.id}><div><b>{item.seatLabel ?? (item.sectionType === 'GENERAL_ADMISSION' ? 'Admisión general' : 'Boleto')}</b><small>{item.sectionName ?? 'Sección'}{item.eventName ? ` · ${item.eventName}` : ''}</small></div><strong>{formatMoney(item.unitPrice, item.currency)}</strong></div>)}</div>}

    {!!selections.length && <div className="cart-totals"><div><span>Boletos</span><b>{selections.length}</b></div><div className="cart-total"><span>Total</span><b>{formatMoney(String(total), currency)}</b></div></div>}

    {order && <><div className="order-status-card"><div><small>ESTADO DE LA ORDEN</small><b>{order.status}</b></div><span>{formatMoney(order.amount, order.currency)}</span></div>{order.status === 'RESERVED' && <ReservationTimer reservedUntil={order.reservedUntil} />}</>}

    {!order && !!selections.length && <button className="button primary full button-lg" disabled={busy} onClick={onContinue}>{busy ? 'Reservando…' : 'Continuar compra'}</button>}
    {order?.status === 'RESERVED' && <div className="stack"><button className="button primary full button-lg" disabled={busy} onClick={onConfirm}>Confirmar compra</button><button className="button secondary full" disabled={busy} onClick={onCancel}>Cancelar reservación</button></div>}
    {order?.status === 'CONFIRMED' && <div className="alert success">Compra confirmada. Tus boletos ya están disponibles en tu cuenta.</div>}
    {order?.status === 'CANCELLED' && <div className="alert info">La reservación fue cancelada.</div>}
    <p className="secure-note"><span>✓</span> Tu selección se reserva al continuar con la compra.</p>
  </aside>
}
