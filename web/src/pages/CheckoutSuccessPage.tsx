import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { myTicketsApi } from '../api/myTicketsApi'
import { ordersApi } from '../api/ordersApi'
import { ErrorState, Loading } from '../components/Ui'

export function CheckoutSuccessPage(){
  const[p]=useSearchParams(); const id=p.get('orderId')
  const q=useQuery({queryKey:['order',id],queryFn:()=>ordersApi.get(id!),enabled:!!id,refetchInterval:x=>x.state.data?.status==='RESERVED'?1500:false})
  const tickets=useQuery({queryKey:['my-tickets','order',id],queryFn:myTicketsApi.getMine,enabled:Boolean(id&&q.data?.status==='CONFIRMED')})
  if(!id)return <main className="page"><ErrorState error={new Error('Orden inválida')}/></main>
  if(q.isPending)return <Loading text="Verificando el estado de tu pago…"/>
  if(q.isError)return <ErrorState error={q.error}/>
  if(q.data.status==='RESERVED')return <main className="page"><section className="panel payment-wait"><span className="payment-spinner"/><span className="eyebrow">Pago en proceso</span><h1>Estamos confirmando tu pago</h1><p className="muted">No necesitas volver a pagar. Esta página consulta el estado real de la orden y se actualizará automáticamente.</p><Link className="button secondary" to="/orders">Ver mis órdenes</Link></section></main>
  if(q.data.status!=='CONFIRMED')return <main className="page"><section className="panel payment-wait"><span className="eyebrow">Compra no confirmada</span><h1>No pudimos confirmar esta orden</h1><p className="muted">Estado actual: {q.data.status}. No intentes reutilizar una reservación vencida; consulta la disponibilidad actual del evento.</p><div className="inline-actions"><Link className="button primary" to="/events">Explorar eventos</Link><Link className="button secondary" to="/orders">Ver orden</Link></div></section></main>
  const issued=(tickets.data??[]).filter(ticket=>ticket.orderId===id)
  return <main className="page"><section className="panel payment-success"><div className="success-mark">✓</div><span className="eyebrow">Pago confirmado</span><h1>¡Tus boletos están listos!</h1><p className="muted">La orden fue confirmada y los accesos ya fueron emitidos.</p>{tickets.isPending&&<p className="muted">Cargando tus accesos…</p>}{issued.length>0&&<div className="success-ticket-list">{issued.map(ticket=><Link key={ticket.id} className="success-ticket-link" to={`/tickets/${ticket.id}`}><span><small>{ticket.sectionName??'General'}</small><b>{ticket.seatLabel?`Asiento ${ticket.seatLabel}`:'Admisión general'}</b></span><strong>Ver boleto →</strong></Link>)}</div>}<div className="inline-actions"><Link className="button primary" to="/tickets">Ver mis boletos</Link><Link className="button secondary" to="/orders">Ver orden</Link></div></section></main>
}
