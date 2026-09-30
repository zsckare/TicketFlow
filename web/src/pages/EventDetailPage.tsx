
import { useMemo, useState } from 'react'
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import { eventsApi } from '../api/eventsApi'
import { ordersApi } from '../api/ordersApi'
import { ticketsApi } from '../api/ticketsApi'
import { ErrorState, Loading } from '../components/Ui'
import { useAuth } from '../features/auth/AuthContext'
import { formatDate, formatMoney } from '../lib/format'

import type { OrderResponse } from '../types/orders'
import type { TicketInventoryResponse } from '../types/tickets'

export function EventDetailPage() {
  const { eventId = '' } = useParams()

  const qc = useQueryClient()
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [sectionId, setSectionId] = useState<string>()
  const [selected, setSelected] =
    useState<TicketInventoryResponse[]>([])
  const [order, setOrder] =
    useState<OrderResponse>()

  /*
   * Event
   */
  const event = useQuery({
    queryKey: ['event', eventId],
    queryFn: () =>
      eventsApi.getEvent(eventId),
  })

  /*
   * Venue
   */
  const venue = useQuery({
    queryKey: [
      'venue',
      event.data?.venueId,
    ],
    queryFn: () =>
      eventsApi.getVenue(
        event.data!.venueId,
      ),
    enabled: !!event.data,
  })

  /*
   * Sections belonging to the venue.
   */
  const sections = useQuery({
    queryKey: [
      'sections',
      event.data?.venueId,
    ],
    queryFn: () =>
      eventsApi.getSections(
        event.data!.venueId,
      ),
    enabled: !!event.data,
  })

  /*
   * If the user hasn't selected a section yet,
   * use the first available section.
   */
  const activeSection =
    sectionId ??
    sections.data?.[0]?.id

  const activeSectionData =
    sections.data?.find(
      section =>
        section.id === activeSection,
    )

  /*
   * Physical seats are only relevant for
   * RESERVED_SEATING sections.
   */
  const seats = useQuery({
    queryKey: [
      'seats',
      activeSection,
    ],
    queryFn: () =>
      eventsApi.getSeats(
        activeSection!,
      ),
    enabled:
      !!activeSection &&
      activeSectionData?.type !==
        'GENERAL_ADMISSION',
  })

  /*
   * Ticket inventory for the complete event.
   *
   * RESERVED_SEATING units have seatId.
   * GENERAL_ADMISSION units have seatId = null.
   */
  const inventory = useQuery({
    queryKey: [
      'inventory',
      eventId,
    ],
    queryFn: () =>
      ticketsApi.getEventInventory(
        eventId,
      ),
  })

  const refreshInventory = () =>
    qc.invalidateQueries({
      queryKey: [
        'inventory',
        eventId,
      ],
    })

  /*
   * Create a reservation/order using the selected
   * inventory IDs.
   */
  const create = useMutation({
    mutationFn: ordersApi.create,

    onSuccess: orderResponse => {
      setOrder(orderResponse)

      void refreshInventory()
    },
  })

  /*
   * Confirm payment/order.
   */
  const confirm = useMutation({
    mutationFn: ordersApi.confirm,

    onSuccess: orderResponse => {
      setOrder(orderResponse)

      void refreshInventory()
    },
  })

  /*
   * Cancel reservation/order.
   */
  const cancel = useMutation({
    mutationFn: ordersApi.cancel,

    onSuccess: orderResponse => {
      setOrder(orderResponse)

      void refreshInventory()
    },
  })

  /*
   * Map physical seat ID -> ticket inventory unit.
   *
   * This allows the seat map to determine whether
   * each physical seat is AVAILABLE, RESERVED or SOLD.
   */
  const seatInventoryMap =
    useMemo(
      () =>
        new Map(
          (inventory.data ?? [])
            .filter(
              item =>
                !!item.seatId,
            )
            .map(item => [
              item.seatId!,
              item,
            ]),
        ),
      [inventory.data],
    )

  /*
   * All AVAILABLE General Admission inventory units
   * belonging to the currently selected section.
   *
   * Each GA ticket is still represented by one
   * inventory row even though it has no physical seat.
   */
  const generalAvailable =
    useMemo(
      () =>
        (inventory.data ?? [])
          .filter(
            item =>
              item.sectionId ===
                activeSection &&
              !item.seatId &&
              item.status ===
                'AVAILABLE',
          ),
      [
        inventory.data,
        activeSection,
      ],
    )

  /*
   * GA units currently selected from the active
   * General Admission section.
   */
  const selectedGeneral =
    useMemo(
      () =>
        selected.filter(
          item =>
            item.sectionId ===
              activeSection &&
            !item.seatId,
        ),
      [
        selected,
        activeSection,
      ],
    )

  /*
   * Add one GA ticket.
   *
   * We select an actual AVAILABLE inventory row so
   * Orders receives a real inventoryId. No fake
   * seat IDs or quantity-only inventory is needed.
   */
  const addGeneralTicket = () => {
    const next =
      generalAvailable.find(
        item =>
          !selected.some(
            selectedItem =>
              selectedItem.id ===
              item.id,
          ),
      )

    if (!next) {
      return
    }

    setSelected(previous => [
      ...previous,
      next,
    ])
  }

  /*
   * Remove the most recently selected GA unit from
   * the active section.
   */
  const removeGeneralTicket =
    () => {
      const last =
        selectedGeneral[
          selectedGeneral.length -
            1
        ]

      if (!last) {
        return
      }

      setSelected(previous =>
        previous.filter(
          item =>
            item.id !== last.id,
        ),
      )
    }

  /*
   * Toggle a numbered seat.
   */
  const toggleSeat = (
    item:
      TicketInventoryResponse,
  ) => {
    setSelected(previous => {
      const alreadySelected =
        previous.some(
          selectedItem =>
            selectedItem.id ===
            item.id,
        )

      if (alreadySelected) {
        return previous.filter(
          selectedItem =>
            selectedItem.id !==
            item.id,
        )
      }

      return [
        ...previous,
        item,
      ]
    })
  }

  /*
   * Total is calculated here only for display.
   *
   * Orders must still calculate/validate its own
   * authoritative total on the backend.
   */
  const total =
    useMemo(
      () =>
        selected.reduce(
          (sum, item) =>
            sum +
            Number(item.price),
          0,
        ),
      [selected],
    )

  if (event.isPending) {
    return <Loading />
  }

  if (event.isError) {
    return (
      <ErrorState
        error={event.error}
      />
    )
  }

  return (
    <main className="page">
      <Link
        className="back"
        to="/events"
      >
        ← Eventos
      </Link>

      <section className="detail-hero">
        <span className="eyebrow">
          Evento
        </span>

        <h1>
          {event.data.name}
        </h1>

        <p>
          {event.data.description}
        </p>

        <div className="meta">
          <span>
            {formatDate(
              event.data.startsAt,
            )}
          </span>

          {venue.data && (
            <span>
              {venue.data.name}
              {' · '}
              {venue.data.city}
            </span>
          )}
        </div>
      </section>

      <div className="booking">
        <section className="panel">
          <div className="section-title">
            <h2>
              Selecciona tus boletos
            </h2>
          </div>

          <div className="tabs">
            {sections.data?.map(
              section => (
                <button
                  key={section.id}
                  className={
                    activeSection ===
                    section.id
                      ? 'active'
                      : ''
                  }
                  onClick={() => {
                    /*
                     * IMPORTANT:
                     *
                     * Do NOT clear selected here.
                     * This allows an order to contain
                     * tickets from multiple sections.
                     */
                    setSectionId(
                      section.id,
                    )
                  }}
                >
                  {section.name}
                </button>
              ),
            )}
          </div>

          {activeSectionData?.type ===
          'GENERAL_ADMISSION' ? (
            <div className="panel">
              <h3>
                Admisión general
              </h3>

              <p className="muted">
                {
                  generalAvailable.length
                }{' '}
                boletos disponibles
              </p>

              {generalAvailable.length >
              0 ? (
                <>
                  <div
                    style={{
                      display: 'flex',
                      alignItems:
                        'center',
                      gap: '1rem',
                      marginTop:
                        '1rem',
                    }}
                  >
                    <button
                      type="button"
                      className="button secondary"
                      disabled={
                        !!order ||
                        selectedGeneral.length ===
                          0
                      }
                      onClick={
                        removeGeneralTicket
                      }
                    >
                      −
                    </button>

                    <strong>
                      {
                        selectedGeneral.length
                      }
                    </strong>

                    <button
                      type="button"
                      className="button primary"
                      disabled={
                        !!order ||
                        selectedGeneral.length >=
                          generalAvailable.length
                      }
                      onClick={
                        addGeneralTicket
                      }
                    >
                      +
                    </button>
                  </div>

                  {generalAvailable[0] && (
                    <p className="muted">
                      {formatMoney(
                        generalAvailable[
                          0
                        ].price,
                        generalAvailable[
                          0
                        ].currency,
                      )}{' '}
                      por boleto
                    </p>
                  )}
                </>
              ) : (
                <div className="alert info">
                  No hay boletos
                  disponibles en esta
                  sección.
                </div>
              )}
            </div>
          ) : (
            <>
              <div className="stage">
                ESCENARIO
              </div>

              <div className="seat-grid">
                {seats.data?.map(
                  seat => {
                    const item =
                      seatInventoryMap.get(
                        seat.id,
                      )

                    const available =
                      item?.status ===
                      'AVAILABLE'

                    const isSelected =
                      selected.some(
                        selectedItem =>
                          selectedItem.id ===
                          item?.id,
                      )

                    return (
                      <button
                        key={seat.id}
                        disabled={
                          !available ||
                          !!order
                        }
                        onClick={() => {
                          if (!item) {
                            return
                          }

                          toggleSeat(
                            item,
                          )
                        }}
                        className={`seat ${
                          available
                            ? 'available'
                            : 'unavailable'
                        } ${
                          isSelected
                            ? 'selected'
                            : ''
                        }`}
                      >
                        <b>
                          {seat.row}
                          {seat.number}
                        </b>

                        <small>
                          {item?.status ??
                            'SIN BOLETO'}
                        </small>
                      </button>
                    )
                  },
                )}
              </div>
            </>
          )}
        </section>

        <aside className="panel checkout">
          <span className="eyebrow">
            Resumen
          </span>

          <h2>
            Tus boletos
          </h2>

          {!selected.length &&
            !order && (
              <p className="muted">
                Selecciona uno o más
                boletos.
              </p>
            )}

          {selected.length > 0 && (
            <>
              <div className="price">
                <span>
                  Boletos
                </span>

                <b>
                  {selected.length}
                </b>
              </div>

              <div className="price">
                <span>
                  Total
                </span>

                <b>
                  {formatMoney(
                    total.toFixed(2),
                    selected[0]
                      .currency,
                  )}
                </b>
              </div>
            </>
          )}

          {order && (
            <div className="order-box">
              <small>
                ORDEN
              </small>

              <b>
                {order.status}
              </b>

              <code>
                {order.id}
              </code>
            </div>
          )}

          {!order &&
            selected.length > 0 && (
              <button
                className="button primary full"
                disabled={
                  create.isPending
                }
                onClick={() => {
                  if (!user) {
                    navigate(
                      '/login',
                      {
                        state: {
                          from:
                            location.pathname,
                        },
                      },
                    )

                    return
                  }

                  /*
                   * Orders receives the actual inventory
                   * IDs for both reserved seats and GA.
                   */
                  create.mutate(
                    selected.map(
                      item =>
                        item.id,
                    ),
                  )
                }}
              >
                {user
                  ? create.isPending
                    ? 'Reservando...'
                    : 'Reservar'
                  : 'Inicia sesión para reservar'}
              </button>
            )}

          {order?.status ===
            'RESERVED' && (
            <div className="stack">
              <button
                className="button primary full"
                disabled={
                  confirm.isPending
                }
                onClick={() =>
                  confirm.mutate(
                    order.id,
                  )
                }
              >
                Confirmar compra
              </button>

              <button
                className="button secondary full"
                disabled={
                  cancel.isPending
                }
                onClick={() =>
                  cancel.mutate(
                    order.id,
                  )
                }
              >
                Cancelar
              </button>
            </div>
          )}

          {order?.status ===
            'CONFIRMED' && (
            <div className="alert success">
              Compra confirmada.
            </div>
          )}

          {order?.status ===
            'CANCELLED' && (
            <div className="alert info">
              Reservación
              cancelada.
            </div>
          )}

          {(create.error ||
            confirm.error ||
            cancel.error) && (
            <ErrorState
              error={
                (create.error ||
                  confirm.error ||
                  cancel.error)!
              }
            />
          )}
        </aside>
      </div>
    </main>
  )
}
