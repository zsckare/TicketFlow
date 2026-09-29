import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ordersApi } from '../../api/ordersApi'
export function DashboardPage() {
  const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents})
  const venues=useQuery({queryKey:['venues'],queryFn:eventsApi.getVenues})
  const orders=useQuery({queryKey:['orders'],queryFn:ordersApi.getAll})
  const confirmed=orders.data?.filter(o=>o.status==='CONFIRMED').length??0
  return <section><div className="section-title"><h2>Dashboard</h2></div>
    <div className="stats">
      <div><span>Venues</span><b>{venues.data?.length??'—'}</b></div>
      <div><span>Eventos</span><b>{events.data?.length??'—'}</b></div>
      <div><span>Órdenes</span><b>{orders.data?.length??'—'}</b></div>
      <div><span>Confirmadas</span><b>{confirmed}</b></div>
    </div>
  </section>
}
