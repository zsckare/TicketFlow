import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { Badge, Empty, ErrorState, Loading } from '../../components/Ui'
import { formatDate } from '../../lib/format'

export function AdminEventsPage() {
  const qc=useQueryClient(); const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents}); const venues=useQuery({queryKey:['venues'],queryFn:eventsApi.getVenues})
  const [venueId,setVenueId]=useState(''); const [name,setName]=useState(''); const [description,setDescription]=useState(''); const [startsAt,setStartsAt]=useState(''); const [endsAt,setEndsAt]=useState('')
  const create=useMutation({mutationFn:eventsApi.createEvent,onSuccess:()=>{setName('');setDescription('');setStartsAt('');setEndsAt('');void qc.invalidateQueries({queryKey:['events']})}})
  const publish=useMutation({mutationFn:eventsApi.publishEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  const cancel=useMutation({mutationFn:eventsApi.cancelEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  const remove=useMutation({mutationFn:eventsApi.deleteEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  if(events.isPending)return <Loading/>; if(events.isError)return <ErrorState error={events.error}/>
  const submit=(e:FormEvent)=>{e.preventDefault();create.mutate({venueId,name,description,startsAt:new Date(startsAt).toISOString(),endsAt:new Date(endsAt).toISOString()})}
  const actionError=publish.error??cancel.error??remove.error
  return <section><div className="section-title"><div><span className="eyebrow">Programación</span><h2>Eventos</h2><p className="muted">Crea borradores, prepara su inventario y publícalos cuando estén listos.</p></div><span className="count-label">{events.data.length} eventos</span></div>{actionError&&<ErrorState error={actionError}/>}<div className="admin-grid">
    <div className="panel"><span className="eyebrow">Nuevo</span><h3>Crear evento</h3><p className="muted">El evento se crea como borrador. Después configura su inventario antes de publicarlo.</p><form className="form" onSubmit={submit}>
      <label>Venue<select required value={venueId} onChange={e=>setVenueId(e.target.value)}><option value="">Selecciona un venue…</option>{venues.data?.map(v=><option key={v.id} value={v.id}>{v.name}</option>)}</select></label>
      <label>Nombre<input placeholder="Nombre del evento" required value={name} onChange={e=>setName(e.target.value)}/></label>
      <label>Descripción<textarea placeholder="Describe brevemente la experiencia" value={description} onChange={e=>setDescription(e.target.value)}/></label>
      <div className="form-row"><label>Inicio<input required type="datetime-local" value={startsAt} onChange={e=>setStartsAt(e.target.value)}/></label><label>Fin<input required type="datetime-local" value={endsAt} onChange={e=>setEndsAt(e.target.value)}/></label></div>
      <button className="button primary" disabled={create.isPending}>{create.isPending?'Creando…':'Crear borrador'}</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
    <div className="panel"><div className="section-title compact"><div><span className="eyebrow">Calendario</span><h3>Todos los eventos</h3></div></div>{!events.data.length?<Empty>No hay eventos todavía.</Empty>:<div className="list event-admin-list">{events.data.map(event=><div className="list-row" key={event.id}><div><div className="list-title-line"><b>{event.name}</b><Badge tone={event.status.toLowerCase()}>{event.status}</Badge></div><small>{formatDate(event.startsAt)}</small>{event.status==='DRAFT'&&<small className="draft-help">Configura inventario y precios antes de publicar.</small>}</div><div className="inline-actions">{event.status==='DRAFT'&&<button disabled={publish.isPending||remove.isPending} onClick={()=>publish.mutate(event.id)}>Publicar</button>}{event.status==='PUBLISHED'&&<button disabled={cancel.isPending} onClick={()=>{if(window.confirm('¿Deshabilitar este evento? Dejará de aparecer en la cartelera pública.'))cancel.mutate(event.id)}}>Deshabilitar</button>}{event.status==='DRAFT'&&<button className="danger-button" disabled={remove.isPending||publish.isPending} onClick={()=>{if(window.confirm('¿Eliminar definitivamente este borrador y su inventario disponible? Esta acción no se puede deshacer.'))remove.mutate(event.id)}}>Eliminar</button>}</div></div>)}</div>}</div></div>
  </section>
}
