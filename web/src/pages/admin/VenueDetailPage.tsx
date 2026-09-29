import {  useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ErrorState, Loading } from '../../components/Ui'
import type { VenueSectionType } from '../../types/events'
export function VenueDetailPage() {
  const {venueId=''}=useParams(); const qc=useQueryClient()
  const venue=useQuery({queryKey:['venue',venueId],queryFn:()=>eventsApi.getVenue(venueId)})
  const sections=useQuery({queryKey:['sections',venueId],queryFn:()=>eventsApi.getSections(venueId)})
  const [name,setName]=useState(''); const [type,setType]=useState<VenueSectionType>('RESERVED_SEATING'); const [capacity,setCapacity]=useState(100)
  const create=useMutation({mutationFn:()=>eventsApi.createSection(venueId,{name,type,capacity}),onSuccess:()=>{setName('');void qc.invalidateQueries({queryKey:['sections',venueId]})}})
  if(venue.isPending)return <Loading/>; if(venue.isError)return <ErrorState error={venue.error}/>
  return <section><div className="section-title"><div><span className="eyebrow">Venue</span><h2>{venue.data.name}</h2><p className="muted">{venue.data.address} · {venue.data.city}</p></div></div>
    <div className="admin-grid"><div className="panel"><h3>Nueva sección</h3><form className="form" onSubmit={(e:FormEvent)=>{e.preventDefault();create.mutate()}}>
      <label>Nombre<input value={name} onChange={e=>setName(e.target.value)} required/></label>
      <label>Tipo<select value={type} onChange={e=>setType(e.target.value as VenueSectionType)}><option>RESERVED_SEATING</option><option>GENERAL_ADMISSION</option></select></label>
      <label>Capacidad<input type="number" min="1" value={capacity} onChange={e=>setCapacity(Number(e.target.value))}/></label>
      <button className="button primary">Crear sección</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
      <div className="panel"><h3>Secciones</h3><div className="list">{sections.data?.map(s=><a key={s.id} href={`/admin/sections/${s.id}`}><div><b>{s.name}</b><small>{s.type} · {s.capacity} lugares</small></div><span>→</span></a>)}</div></div>
    </div>
  </section>
}
