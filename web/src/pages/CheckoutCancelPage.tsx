import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { ordersApi } from '../api/ordersApi'
import { ErrorState, Loading } from '../components/Ui'
import { ReservationTimer } from '../features/booking/ReservationTimer'

export function CheckoutCancelPage() {
  const [params] = useSearchParams()
  const orderId = params.get('orderId')
  const order = useQuery({ queryKey:['order',orderId], queryFn:()=>ordersApi.get(orderId!), enabled:!!orderId })
  if (!orderId) return <main className="page"><ErrorState error={new Error('Orden inválida')}/></main>
  if (order.isPending) return <Loading text="Recuperando tu orden…"/>
  if (order.isError) return <ErrorState error={order.error}/>
  const data=order.data
  const stillReserved=data.status==='RESERVED' && (!data.reservedUntil || new Date(data.reservedUntil).getTime()>Date.now())
  return <main className="page"><section className="panel payment-wait">
    <span className="eyebrow">Pago cancelado</span>
    <h1>{stillReserved ? 'Tu compra no fue cobrada' : 'La reservación ya no está disponible'}</h1>
    <p className="muted">{stillReserved ? 'Tu reservación sigue activa por el tiempo restante. Puedes volver al checkout e intentarlo nuevamente.' : 'No se confirmó ningún pago. Regresa al catálogo para consultar disponibilidad actual.'}</p>
    {stillReserved && <ReservationTimer reservedUntil={data.reservedUntil}/>} 
    <div className="inline-actions">{stillReserved && <Link className="button primary" to="/checkout">Volver al checkout</Link>}<Link className="button secondary" to="/events">Explorar eventos</Link></div>
  </section></main>
}
