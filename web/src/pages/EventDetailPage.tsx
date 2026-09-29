import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useEvent, useSections, useSeats, useVenue } from '../features/events/eventQueries'
import { useEventInventory } from '../features/tickets/ticketQueries'
import { useCancelOrder, useConfirmOrder, useCreateOrder } from '../features/orders/orderMutations'
import type { TicketInventoryResponse } from '../types/tickets'
import type { OrderResponse } from '../types/orders'

function formatMoney(value: string, currency: string) {
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency }).format(Number(value))
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-MX', { dateStyle: 'full', timeStyle: 'short' }).format(new Date(value))
}

export function EventDetailPage() {
  const { eventId } = useParams()
  const [selectedSectionId, setSelectedSectionId] = useState<string>()
  const [selectedInventory, setSelectedInventory] = useState<TicketInventoryResponse>()
  const [activeOrder, setActiveOrder] = useState<OrderResponse>()

  const eventQuery = useEvent(eventId)
  const venueQuery = useVenue(eventQuery.data?.venueId)
  const sectionsQuery = useSections(eventQuery.data?.venueId)
  const inventoryQuery = useEventInventory(eventId)

  const sections = sectionsQuery.data ?? []
  const activeSectionId = selectedSectionId ?? sections[0]?.id
  const seatsQuery = useSeats(activeSectionId)

  const createOrder = useCreateOrder()
  const confirmOrder = useConfirmOrder()
  const cancelOrder = useCancelOrder()

  const inventoryBySeat = useMemo(
    () => new Map((inventoryQuery.data ?? []).map((item) => [item.seatId, item])),
    [inventoryQuery.data],
  )

  if (eventQuery.isPending) return <div className="state-message">Cargando evento…</div>
  if (eventQuery.isError) {
    return <div className="state-message state-message--error">No fue posible cargar el evento: {eventQuery.error.message}</div>
  }

  const event = eventQuery.data

  async function handleReserve() {
    if (!selectedInventory) return
    setActiveOrder(await createOrder.mutateAsync(selectedInventory.id))
  }

  async function handleConfirm() {
    if (!activeOrder) return
    setActiveOrder(await confirmOrder.mutateAsync(activeOrder.id))
  }

  async function handleCancel() {
    if (!activeOrder) return
    setActiveOrder(await cancelOrder.mutateAsync(activeOrder.id))
  }

  const mutationError = createOrder.error ?? confirmOrder.error ?? cancelOrder.error

  return (
    <main className="page">
      <Link className="back-link" to="/events">← Volver a eventos</Link>

      <section className="event-header">
        <div>
          <span className="eyebrow">Evento</span>
          <h1>{event.name}</h1>
          {event.description && <p>{event.description}</p>}
          <div className="event-meta">
            <span>{formatDate(event.startsAt)}</span>
            {venueQuery.data && <span>{venueQuery.data.name} · {venueQuery.data.city}</span>}
          </div>
        </div>
        <div className="event-header__badge">LIVE</div>
      </section>

      <div className="booking-layout">
        <section className="seat-panel">
          <div className="section-heading">
            <div><span className="eyebrow">Boletos</span><h2>Selecciona tu asiento</h2></div>
          </div>

          <div className="section-tabs">
            {sections.map((section) => (
              <button
                className={section.id === activeSectionId ? 'section-tab section-tab--active' : 'section-tab'}
                key={section.id}
                onClick={() => {
                  setSelectedSectionId(section.id)
                  setSelectedInventory(undefined)
                }}
                type="button"
              >
                {section.name}
              </button>
            ))}
          </div>

          <div className="stage">ESCENARIO</div>

          {seatsQuery.isPending || inventoryQuery.isPending ? (
            <div className="state-message">Cargando asientos…</div>
          ) : (
            <div className="seat-grid">
              {(seatsQuery.data ?? []).map((seat) => {
                const inventory = inventoryBySeat.get(seat.id)
                const available = inventory?.status === 'AVAILABLE'
                const selected = selectedInventory?.id === inventory?.id
                return (
                  <button
                    className={['seat', available ? 'seat--available' : 'seat--unavailable', selected ? 'seat--selected' : ''].filter(Boolean).join(' ')}
                    disabled={!available || Boolean(activeOrder)}
                    key={seat.id}
                    onClick={() => setSelectedInventory(inventory)}
                    type="button"
                    title={inventory ? `${seat.row}-${seat.number}: ${inventory.status}` : `${seat.row}-${seat.number}: sin inventario`}
                  >
                    <strong>{seat.row}{seat.number}</strong>
                    <small>{inventory?.status ?? 'NO INVENTORY'}</small>
                  </button>
                )
              })}
            </div>
          )}

          <div className="legend">
            <span><i className="legend-dot legend-dot--available" /> Disponible</span>
            <span><i className="legend-dot legend-dot--selected" /> Seleccionado</span>
            <span><i className="legend-dot legend-dot--sold" /> No disponible</span>
          </div>
        </section>

        <aside className="checkout-card">
          <span className="eyebrow">Tu selección</span>
          <h2>Resumen</h2>

          {!selectedInventory && !activeOrder ? (
            <p className="muted">Selecciona un asiento disponible para continuar.</p>
          ) : (
            <>
              {selectedInventory && (
                <div className="price-row">
                  <span>Precio</span>
                  <strong>{formatMoney(selectedInventory.price, selectedInventory.currency)}</strong>
                </div>
              )}

              {activeOrder && (
                <div className="order-status">
                  <span>Orden</span>
                  <strong>{activeOrder.status}</strong>
                  <small>{activeOrder.id}</small>
                </div>
              )}

              {!activeOrder && selectedInventory && (
                <button className="button button--primary button--full" disabled={createOrder.isPending} onClick={() => void handleReserve()} type="button">
                  {createOrder.isPending ? 'Reservando…' : 'Reservar asiento'}
                </button>
              )}

              {activeOrder?.status === 'RESERVED' && (
                <div className="action-stack">
                  <button className="button button--primary button--full" disabled={confirmOrder.isPending} onClick={() => void handleConfirm()} type="button">
                    {confirmOrder.isPending ? 'Confirmando…' : 'Confirmar compra'}
                  </button>
                  <button className="button button--secondary button--full" disabled={cancelOrder.isPending} onClick={() => void handleCancel()} type="button">
                    {cancelOrder.isPending ? 'Cancelando…' : 'Cancelar reservación'}
                  </button>
                </div>
              )}

              {activeOrder?.status === 'CONFIRMED' && <div className="success-box">Compra confirmada. El boleto ahora está vendido.</div>}
              {activeOrder?.status === 'CANCELLED' && <div className="info-box">Reservación cancelada. El asiento volvió a estar disponible.</div>}
            </>
          )}

          {mutationError && <div className="error-box">{mutationError.message}</div>}
        </aside>
      </div>
    </main>
  )
}
