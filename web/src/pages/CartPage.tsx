import {
  Link,
  useNavigate,
} from 'react-router-dom'

import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import {
  ordersApi,
} from '../api/ordersApi'

import {
  ErrorState,
  Loading,
} from '../components/Ui'

import {
  BookingCart,
} from '../features/booking/BookingCart'

/**
 * Persistent cart backed by the user's active RESERVED order.
 *
 * The server is the source of truth, so the cart survives
 * navigation and browser reloads.
 */
export function CartPage() {
  const navigate =
    useNavigate()

  const qc =
    useQueryClient()

  const active =
    useQuery({
      queryKey: [
        'active-order',
      ],
      queryFn:
        ordersApi.getActive,
    })

  const refresh =
    () => {
      void qc.invalidateQueries({
        queryKey: [
          'active-order',
        ],
      })

      void qc.invalidateQueries({
        queryKey: [
          'orders',
        ],
      })

      void qc.invalidateQueries({
        queryKey: [
          'my-orders',
        ],
      })

      void qc.invalidateQueries({
        queryKey: [
          'inventory',
        ],
      })
    }

  const cancel =
    useMutation({
      mutationFn:
        ordersApi.cancel,

      onSuccess:
        refresh,
    })

  if (active.isPending) {
    return (
      <Loading
        text="Cargando tu carrito…"
      />
    )
  }

  if (active.isError) {
    return (
      <ErrorState
        error={active.error}
      />
    )
  }

  const order =
    active.data

  if (!order) {
    return (
      <main className="page">
        <section className="panel cart-page-empty">
          <span className="eyebrow">
            Carrito
          </span>

          <h1>
            Tu carrito está vacío
          </h1>

          <p className="muted">
            Cuando reserves boletos,
            tu orden permanecerá aquí
            mientras la reservación
            siga vigente.
          </p>

          <Link
            className="button primary"
            to="/events"
          >
            Explorar eventos
          </Link>
        </section>
      </main>
    )
  }

  const eventId =
    order.items[0]?.eventId

  return (
    <main className="page cart-page">
      <div className="section-title">
        <div>
          <span className="eyebrow">
            Carrito
          </span>

          <h1>
            Completa tu compra
          </h1>

          <p className="muted">
            Tu reservación se mantiene
            en tu cuenta aunque cambies
            de página o recargues el
            navegador.
          </p>
        </div>

        {eventId && (
          <Link
            className="button secondary"
            to={`/events/${eventId}`}
          >
            Volver al evento
          </Link>
        )}
      </div>

      <BookingCart
        selections={[]}
        order={order}
        busy={cancel.isPending}
        onRemove={() => { }}
        onContinue={() => { }}
        onConfirm={() =>
          navigate('/checkout')
        }
        onCancel={() =>
          cancel.mutate(
            order.id,
          )
        }
      />

      {cancel.error && (
        <ErrorState
          error={cancel.error}
        />
      )}
    </main>
  )
}