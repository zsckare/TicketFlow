import { useCallback, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { ordersApi } from '../api/ordersApi'
import { ErrorState, Loading } from '../components/Ui'
import { ReservationTimer } from '../features/booking/ReservationTimer'
import { formatMoney } from '../lib/format'

export function CheckoutPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [expired, setExpired] = useState(false)
  const active = useQuery({ queryKey: ['active-order'], queryFn: ordersApi.getActive })

  const handleExpire = useCallback(() => {
    setExpired(true)
    void qc.invalidateQueries({ queryKey: ['active-order'] })
    void qc.invalidateQueries({ queryKey: ['inventory'] })
  }, [qc])

  const checkout = useMutation({
    mutationFn: async () => {
      if (!active.data) throw new Error('No active order')
      if (expired || (active.data.reservedUntil && new Date(active.data.reservedUntil).getTime() <= Date.now())) {
        throw new Error('Tu reservación expiró. Selecciona tus boletos nuevamente.')
      }
      return ordersApi.checkout(active.data.id)
    },
    onSuccess: response => {
      if (response.checkoutUrl) {
        window.location.assign(response.checkoutUrl)
        return
      }
      void qc.invalidateQueries({ queryKey: ['active-order'] })
      void qc.invalidateQueries({ queryKey: ['my-orders'] })
      void qc.invalidateQueries({ queryKey: ['my-tickets'] })
      void qc.invalidateQueries({ queryKey: ['inventory'] })
      navigate(`/checkout/success?orderId=${response.order.id}`)
    },
  })

  if (active.isPending) return <Loading text="Recuperando tu reservación…" />
  if (active.isError) return <ErrorState error={active.error} />
  const order = active.data

  if (!order || expired) return <main className="page"><section className="panel empty-feature">
    <span className="eyebrow">Reservación finalizada</span>
    <h1>Tu reservación expiró</h1>
    <p className="muted">Los boletos fueron liberados para que puedan volver a venderse. Puedes regresar al evento y seleccionar disponibilidad actual.</p>
    <Link className="button primary" to={order?.items[0]?.eventId ? `/events/${order.items[0].eventId}` : '/events'}>Volver a los eventos</Link>
  </section></main>

  return <main className="page checkout-page">
    <div className="page-heading"><div><span className="eyebrow">Checkout seguro</span><h1>Revisa y paga</h1><p>La reservación pertenece al servidor y se recupera aunque recargues esta página.</p></div></div>
    <div className="checkout-grid">
      <section className="panel checkout-review">
        <h2>{order.items[0]?.eventName ?? 'Tu evento'}</h2>
        <p className="muted">{order.items[0]?.venueName}</p>
        <div className="checkout-lines">{order.items.map(item => <div key={item.id}><span><b>{item.seatLabel ?? (item.sectionType === 'GENERAL_ADMISSION' ? 'Admisión general' : 'Boleto')}</b><small>{item.sectionName}</small></span><strong>{formatMoney(item.unitPrice,item.currency)}</strong></div>)}</div>
        <ReservationTimer reservedUntil={order.reservedUntil} onExpire={handleExpire}/>
      </section>
      <aside className="panel payment-summary">
        <span className="eyebrow">Resumen</span>
        <div className="summary-row"><span>Boletos</span><b>{order.items.length}</b></div>
        <div className="summary-row total"><span>Total</span><b>{formatMoney(order.amount,order.currency)}</b></div>
        <button className="button primary full button-lg" disabled={checkout.isPending || expired} onClick={()=>checkout.mutate()}>{checkout.isPending ? 'Iniciando pago…' : 'Pagar ahora'}</button>
        <Link className="button secondary full" to="/cart">Volver al carrito</Link>
        <p className="secure-note"><span>✓</span> La orden sólo se confirma después de validar el pago.</p>
        {checkout.isPending && <div className="alert info">No cierres esta ventana mientras preparamos el pago.</div>}
        {checkout.error && <ErrorState error={checkout.error}/>} 
      </aside>
    </div>
  </main>
}
