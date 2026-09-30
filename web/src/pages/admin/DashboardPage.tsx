import {
  useQuery,
} from '@tanstack/react-query'

import {
  Link,
} from 'react-router-dom'

import {
  eventsApi,
} from '../../api/eventsApi'

import {
  ordersApi,
} from '../../api/ordersApi'

import {
  ErrorState,
  Loading,
} from '../../components/Ui'

import {
  formatMoney,
} from '../../lib/format'

export function DashboardPage() {
  const events =
    useQuery({
      queryKey: [
        'events',
      ],
      queryFn:
        eventsApi.getEvents,
    })

  const venues =
    useQuery({
      queryKey: [
        'venues',
      ],
      queryFn:
        eventsApi.getVenues,
    })

  const orders =
    useQuery({
      queryKey: [
        'orders',
      ],
      queryFn:
        ordersApi.getAll,
    })

  /*
   * Wait until all dashboard data has been loaded.
   */
  if (
    events.isPending ||
    venues.isPending ||
    orders.isPending
  ) {
    return (
      <Loading />
    )
  }

  /*
   * Surface the first error returned by any of the
   * dashboard queries.
   */
  const error =
    events.error ??
    venues.error ??
    orders.error

  if (error) {
    return (
      <ErrorState
        error={error}
      />
    )
  }

  /*
   * React Query knows the requests are no longer pending,
   * but TypeScript still allows data to be undefined.
   *
   * Using empty arrays as fallbacks keeps the calculations
   * safe without non-null assertions or unsafe casts.
   */
  const eventData =
    events.data ?? []

  const venueData =
    venues.data ?? []

  const orderData =
    orders.data ?? []

  /*
   * Orders that completed the payment flow.
   */
  const confirmedOrders =
    orderData.filter(
      order =>
        order.status ===
        'CONFIRMED',
    )

  /*
   * Reservations currently waiting for checkout/payment.
   */
  const reserved =
    orderData.filter(
      order =>
        order.status ===
        'RESERVED',
    ).length

  /*
   * Events currently visible in the public catalog.
   */
  const published =
    eventData.filter(
      event =>
        event.status ===
        'PUBLISHED',
    ).length

  /*
   * Each confirmed OrderItem represents one sold ticket.
   */
  const soldTickets =
    confirmedOrders.reduce(
      (
        total,
        order,
      ) =>
        total +
        order.items.length,
      0,
    )

  /*
   * Revenue is grouped by currency so we do not
   * accidentally add amounts from different currencies.
   */
  const revenueByCurrency =
    confirmedOrders.reduce<
      Record<string, number>
    >(
      (
        accumulator,
        order,
      ) => {
        accumulator[
          order.currency
        ] =
          (
            accumulator[
            order.currency
            ] ??
            0
          ) +
          Number(
            order.amount,
          )

        return accumulator
      },
      {},
    )

  /*
   * The current dashboard displays the first currency.
   * Later we can render one metric per currency if the
   * platform supports multiple currencies simultaneously.
   */
  const primaryRevenue =
    Object.entries(
      revenueByCurrency,
    )[0]

  /*
   * Show only the five most recent entries returned
   * by Orders Service.
   */
  const recentOrders =
    orderData.slice(
      0,
      5,
    )

  return (
    <section>
      <div className="section-title">
        <div>
          <span className="eyebrow">
            Resumen
          </span>

          <h2>
            Estado de la operación
          </h2>

          <p className="muted">
            Ventas, reservaciones y
            actividad reciente de
            TicketFlow.
          </p>
        </div>
      </div>

      <div className="stats">
        <div>
          <span>
            Ingresos confirmados
          </span>

          <b>
            {primaryRevenue
              ? formatMoney(
                String(
                  primaryRevenue[1],
                ),
                primaryRevenue[0],
              )
              : '—'}
          </b>

          <small>
            {confirmedOrders.length}{' '}
            órdenes pagadas
          </small>
        </div>

        <div>
          <span>
            Boletos vendidos
          </span>

          <b>
            {soldTickets}
          </b>

          <small>
            Accesos emitidos
          </small>
        </div>

        <div>
          <span>
            Reservaciones activas
          </span>

          <b>
            {reserved}
          </b>

          <small>
            Pendientes de completar
          </small>
        </div>

        <div>
          <span>
            Eventos publicados
          </span>

          <b>
            {published}
          </b>

          <small>
            {eventData.length}{' '}
            eventos totales ·{' '}
            {venueData.length}{' '}
            venues
          </small>
        </div>
      </div>

      <div className="admin-dashboard-grid">
        <div className="panel">
          <div className="section-title">
            <div>
              <h3>
                Actividad reciente
              </h3>

              <p className="muted">
                Últimas órdenes
                registradas.
              </p>
            </div>

            <Link to="/admin/orders">
              Ver todas
            </Link>
          </div>

          {recentOrders.length ? (
            <div className="dashboard-list">
              {recentOrders.map(
                order => (
                  <div
                    key={
                      order.id
                    }
                  >
                    <div>
                      <b>
                        {order
                          .items[0]
                          ?.eventName ??
                          `Orden #${order.id.slice(
                            0,
                            8,
                          )}`}
                      </b>

                      <small>
                        {
                          order
                            .items
                            .length
                        }{' '}
                        boletos ·{' '}
                        {
                          order.status
                        }
                      </small>
                    </div>

                    <strong>
                      {formatMoney(
                        order.amount,
                        order.currency,
                      )}
                    </strong>
                  </div>
                ),
              )}
            </div>
          ) : (
            <p className="muted">
              Todavía no hay
              órdenes.
            </p>
          )}
        </div>

        <div className="panel">
          <div className="section-title">
            <div>
              <h3>
                Acciones rápidas
              </h3>

              <p className="muted">
                Continúa con las
                tareas más
                frecuentes.
              </p>
            </div>
          </div>

          <div className="quick-actions">
            <Link
              className="button secondary"
              to="/admin/events"
            >
              Gestionar eventos
            </Link>

            <Link
              className="button secondary"
              to="/admin/inventory"
            >
              Configurar inventario
            </Link>

            <Link
              className="button primary"
              to="/admin/check-in"
            >
              Abrir check-in
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}