import {  useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { Badge, ErrorState, Loading } from '../../components/Ui'
import { formatDate } from '../../lib/format'
export function AdminEventsPage() {
  const qc=useQueryClient(); const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents}); const venues=useQuery({queryKey:['venues'],queryFn:eventsApi.getVenues})
  const [venueId,setVenueId]=useState(''); const [name,setName]=useState(''); const [description,setDescription]=useState(''); const [startsAt,setStartsAt]=useState(''); const [endsAt,setEndsAt]=useState('')
  const create=useMutation({mutationFn:eventsApi.createEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  const publish=useMutation({mutationFn:eventsApi.publishEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  const cancel=useMutation({mutationFn:eventsApi.cancelEvent,onSuccess:()=>void qc.invalidateQueries({queryKey:['events']})})
  if(events.isPending)return <Loading/>; if(events.isError)return <ErrorState error={events.error}/>
  const submit=(e:FormEvent)=>{e.preventDefault();create.mutate({venueId,name,description,startsAt:new Date(startsAt).toISOString(),endsAt:new Date(endsAt).toISOString()})}
  return <section><div className="section-title"><h2>Eventos</h2></div><div className="admin-grid">
    <div className="panel"><h3>Nuevo evento</h3><form className="form" onSubmit={submit}>
      <label>Venue<select required value={venueId} onChange={e=>setVenueId(e.target.value)}><option value="">Selecciona…</option>{venues.data?.map(v=><option key={v.id} value={v.id}>{v.name}</option>)}</select></label>
      <label>Nombre<input required value={name} onChange={e=>setName(e.target.value)}/></label>
      <label>Descripción<textarea value={description} onChange={e=>setDescription(e.target.value)}/></label>
      <label>Inicio<input required type="datetime-local" value={startsAt} onChange={e=>setStartsAt(e.target.value)}/></label>
      <label>Fin<input required type="datetime-local" value={endsAt} onChange={e=>setEndsAt(e.target.value)}/></label>
      <button className="button primary">Crear borrador</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
    <div className="panel"><h3>Todos los eventos</h3><div className="list">{events.data.map(e=><div className="list-row" key={e.id}>
      <div><b>{e.name}</b><small>{formatDate(e.startsAt)}</small><Badge tone={e.status.toLowerCase()}>{e.status}</Badge></div>
      <div className="inline-actions">{e.status==='DRAFT'&&<button onClick={()=>publish.mutate(e.id)}>Publicar</button>}{!['CANCELLED','FINISHED'].includes(e.status)&&<button onClick={()=>cancel.mutate(e.id)}>Cancelar</button>}</div>
    </div>)}</div></div></div>
  </section>
}
