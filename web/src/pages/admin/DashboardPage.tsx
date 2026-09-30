import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { eventsApi } from '../../api/eventsApi'
import { ordersApi } from '../../api/ordersApi'
import { ErrorState, Loading } from '../../components/Ui'
import { formatMoney } from '../../lib/format'

export function DashboardPage() {
  const events = useQuery({ queryKey: ['events'], queryFn: eventsApi.getEvents })
  const venues = useQuery({ queryKey: ['venues'], queryFn: eventsApi.getVenues })
  const orders = useQuery({ queryKey: ['orders'], queryFn: ordersApi.getAll })
  if (events.isPending || venues.isPending || orders.isPending) return <Loading />
  const error = events.error ?? venues.error ?? orders.error
  if (error) return <ErrorState error={error} />

  const orderData = orders.data ?? []
  const eventData = events.data ?? []
  const venueData = venues.data ?? []

  const confirmedOrders = orderData.filter(o => o.status === 'CONFIRMED')
  const reserved = orderData.filter(o => o.status === 'RESERVED').length
  const published = eventData.filter(e => e.status === 'PUBLISHED').length
  const soldTickets = confirmedOrders.reduce((sum, order) => sum + order.items.length, 0)
  const revenueByCurrency = confirmedOrders.reduce<Record<string, number>>((acc, order) => { acc[order.currency] = (acc[order.currency] ?? 0) + Number(order.amount); return acc }, {})
  const primaryRevenue = Object.entries(revenueByCurrency)[0]
  const recentOrders = orderData.slice(0, 5)

  return <section>
    <div className="section-title"><div><span className="eyebrow">Resumen</span><h2>Estado de la operación</h2><p className="muted">Ventas, reservaciones y actividad reciente de TicketFlow.</p></div></div>
    <div className="stats"><div><span>Ingresos confirmados</span><b>{primaryRevenue ? formatMoney(String(primaryRevenue[1]), primaryRevenue[0]) : '—'}</b><small>{confirmedOrders.length} órdenes pagadas</small></div><div><span>Boletos vendidos</span><b>{soldTickets}</b><small>Accesos emitidos</small></div><div><span>Reservaciones activas</span><b>{reserved}</b><small>Pendientes de completar</small></div><div><span>Eventos publicados</span><b>{published}</b><small>{eventData.length} eventos totales · {venueData.length} venues</small></div></div>
    <div className="admin-dashboard-grid">
      <div className="panel"><div className="section-title"><div><h3>Actividad reciente</h3><p className="muted">Últimas órdenes registradas.</p></div><Link to="/admin/orders">Ver todas</Link></div>{recentOrders.length ? <div className="dashboard-list">{recentOrders.map(order => <div key={order.id}><div><b>{order.items[0]?.eventName ?? `Orden #${order.id.slice(0, 8)}`}</b><small>{order.items.length} boletos · {order.status}</small></div><strong>{formatMoney(order.amount, order.currency)}</strong></div>)}</div> : <p className="muted">Todavía no hay órdenes.</p>}</div>
      <div className="panel"><div className="section-title"><div><h3>Acciones rápidas</h3><p className="muted">Continúa con las tareas más frecuentes.</p></div></div><div className="quick-actions"><Link className="button secondary" to="/admin/events">Gestionar eventos</Link><Link className="button secondary" to="/admin/inventory">Configurar inventario</Link><Link className="button primary" to="/admin/check-in">Abrir check-in</Link></div></div>
    </div>
  </section>
}
