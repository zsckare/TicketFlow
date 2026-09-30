import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ordersApi } from '../../api/ordersApi'
export function DashboardPage() {
  const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents}); const venues=useQuery({queryKey:['venues'],queryFn:eventsApi.getVenues}); const orders=useQuery({queryKey:['orders'],queryFn:ordersApi.getAll})
  const confirmed=orders.data?.filter(o=>o.status==='CONFIRMED').length??0; const reserved=orders.data?.filter(o=>o.status==='RESERVED').length??0; const published=events.data?.filter(e=>e.status==='PUBLISHED').length??0
  return <section><div className="section-title"><div><span className="eyebrow">Resumen</span><h2>Estado de la operación</h2><p className="muted">Una vista rápida de la actividad actual de TicketFlow.</p></div></div><div className="stats"><div><span>Venues</span><b>{venues.data?.length??'—'}</b><small>Espacios registrados</small></div><div><span>Eventos publicados</span><b>{published}</b><small>{events.data?.length??0} eventos totales</small></div><div><span>Reservaciones</span><b>{reserved}</b><small>Pendientes de completar</small></div><div><span>Ventas confirmadas</span><b>{confirmed}</b><small>Órdenes completadas</small></div></div></section>
}
