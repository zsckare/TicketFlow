import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'
import { ticketsApi } from '../api/ticketsApi'
import { ordersApi } from '../api/ordersApi'
import { ErrorState, Loading } from '../components/Ui'
import { formatDate, formatMoney } from '../lib/format'
import type { TicketInventoryResponse } from '../types/tickets'
import type { OrderResponse } from '../types/orders'

export function EventDetailPage() {
  const { eventId = '' } = useParams()
  const qc = useQueryClient()
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [sectionId, setSectionId] = useState<string>()
  const [selected, setSelected] = useState<TicketInventoryResponse>()
  const [order, setOrder] = useState<OrderResponse>()
  const event = useQuery({ queryKey:['event',eventId], queryFn:()=>eventsApi.getEvent(eventId) })
  const venue = useQuery({ queryKey:['venue',event.data?.venueId], queryFn:()=>eventsApi.getVenue(event.data!.venueId), enabled:!!event.data })
  const sections = useQuery({ queryKey:['sections',event.data?.venueId], queryFn:()=>eventsApi.getSections(event.data!.venueId), enabled:!!event.data })
  const activeSection = sectionId ?? sections.data?.[0]?.id
  const activeSectionData = sections.data?.find(s => s.id === activeSection)
  const seats = useQuery({ queryKey:['seats',activeSection], queryFn:()=>eventsApi.getSeats(activeSection!), enabled:!!activeSection })
  const inventory = useQuery({ queryKey:['inventory',eventId], queryFn:()=>ticketsApi.getEventInventory(eventId) })
  const refresh = () => qc.invalidateQueries({ queryKey:['inventory',eventId] })
  const create = useMutation({ mutationFn:ordersApi.create, onSuccess:o=>{setOrder(o); void refresh()} })
  const confirm = useMutation({ mutationFn:ordersApi.confirm, onSuccess:o=>{setOrder(o); void refresh()} })
  const cancel = useMutation({ mutationFn:ordersApi.cancel, onSuccess:o=>{setOrder(o); void refresh()} })
  const map = useMemo(()=>new Map((inventory.data??[]).filter(i=>i.seatId).map(i=>[i.seatId!,i])),[inventory.data])
  const generalAvailable = useMemo(() => (inventory.data ?? []).filter(i => i.sectionId === activeSection && !i.seatId && i.status === 'AVAILABLE'), [inventory.data, activeSection])

  if (event.isPending) return <Loading />
  if (event.isError) return <ErrorState error={event.error} />
  return <main className="page">
    <Link className="back" to="/events">← Eventos</Link>
    <section className="detail-hero"><span className="eyebrow">Evento</span><h1>{event.data.name}</h1>
      <p>{event.data.description}</p><div className="meta"><span>{formatDate(event.data.startsAt)}</span>{venue.data&&<span>{venue.data.name} · {venue.data.city}</span>}</div>
    </section>
    <div className="booking">
      <section className="panel">
        <div className="section-title"><h2>Selecciona tu asiento</h2></div>
        <div className="tabs">{sections.data?.map(s=><button key={s.id} className={activeSection===s.id?'active':''} onClick={()=>{setSectionId(s.id);setSelected(undefined)}}>{s.name}</button>)}</div>
        <div className="stage">ESCENARIO</div>
        {activeSectionData?.type === 'GENERAL_ADMISSION' ?
          <div className="panel">
            <h3>Admisión general</h3>
            <p className="muted">{generalAvailable.length} boletos disponibles</p>
            {generalAvailable[0] && <button className="button primary" disabled={!!order} onClick={()=>setSelected(generalAvailable[0])}>Seleccionar boleto</button>}
            {!generalAvailable.length && <div className="alert info">No hay boletos disponibles en esta sección.</div>}
          </div> :
          <div className="seat-grid">{seats.data?.map(s=>{
            const inv=map.get(s.id); const available=inv?.status==='AVAILABLE'
            return <button key={s.id} disabled={!available||!!order} onClick={()=>setSelected(inv)}
              className={`seat ${available?'available':'unavailable'} ${selected?.id===inv?.id?'selected':''}`}>
              <b>{s.row}{s.number}</b><small>{inv?.status??'SIN BOLETO'}</small>
            </button>
          })}</div>}
      </section>
      <aside className="panel checkout"><span className="eyebrow">Resumen</span><h2>Tu boleto</h2>
        {!selected&&!order&&<p className="muted">Selecciona un asiento disponible.</p>}
        {selected&&<div className="price"><span>Precio</span><b>{formatMoney(selected.price,selected.currency)}</b></div>}
        {order&&<div className="order-box"><small>ORDEN</small><b>{order.status}</b><code>{order.id}</code></div>}
        {!order&&selected&&<button className="button primary full" disabled={create.isPending} onClick={()=>{ if (!user) { navigate('/login', { state: { from: location.pathname } }); return } create.mutate(selected.id) }}> {user ? 'Reservar' : 'Inicia sesión para reservar'}</button>}
        {order?.status==='RESERVED'&&<div className="stack">
          <button className="button primary full" onClick={()=>confirm.mutate(order.id)}>Confirmar compra</button>
          <button className="button secondary full" onClick={()=>cancel.mutate(order.id)}>Cancelar</button>
        </div>}
        {order?.status==='CONFIRMED'&&<div className="alert success">Compra confirmada.</div>}
        {order?.status==='CANCELLED'&&<div className="alert info">Reservación cancelada.</div>}
        {(create.error||confirm.error||cancel.error)&&<ErrorState error={(create.error||confirm.error||cancel.error)!}/>}
      </aside>
    </div>
  </main>
}
