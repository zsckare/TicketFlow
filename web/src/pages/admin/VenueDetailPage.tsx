import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { Empty, ErrorState, Loading } from '../../components/Ui'
import type { VenueSectionType } from '../../types/events'

export function VenueDetailPage() {
  const { venueId = '' } = useParams()
  const qc = useQueryClient()
  const venue = useQuery({ queryKey: ['venue', venueId], queryFn: () => eventsApi.getVenue(venueId) })
  const sections = useQuery({ queryKey: ['sections', venueId], queryFn: () => eventsApi.getSections(venueId) })
  const [name,setName]=useState(''); const [type,setType]=useState<VenueSectionType>('RESERVED_SEATING'); const [capacity,setCapacity]=useState(100)
  const create=useMutation({mutationFn:()=>eventsApi.createSection(venueId,{name,type,capacity}),onSuccess:()=>{setName('');void qc.invalidateQueries({queryKey:['sections',venueId]})}})
  if(venue.isPending)return <Loading/>; if(venue.isError)return <ErrorState error={venue.error}/>
  return <section><div className="section-title"><div><span className="eyebrow">Venue</span><h2>{venue.data.name}</h2><p className="muted">{venue.data.address} · {venue.data.city}</p></div><span className="count-label">{sections.data?.length ?? 0} secciones</span></div>
    <div className="admin-grid"><div className="panel"><span className="eyebrow">Configuración</span><h3>Nueva sección</h3><p className="muted">Define cómo se distribuirá la capacidad del venue.</p><form className="form" onSubmit={(e:FormEvent)=>{e.preventDefault();create.mutate()}}><label>Nombre<input placeholder="Ej. Platea A" value={name} onChange={e=>setName(e.target.value)} required/></label><label>Tipo<select value={type} onChange={e=>setType(e.target.value as VenueSectionType)}><option value="RESERVED_SEATING">Asientos numerados</option><option value="GENERAL_ADMISSION">Admisión general</option></select></label><label>Capacidad<input type="number" min="1" value={capacity} onChange={e=>setCapacity(Number(e.target.value))}/><small className="field-hint">Máximo de personas permitido en esta sección.</small></label><button className="button primary" disabled={create.isPending}>{create.isPending?'Creando…':'Crear sección'}</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
      <div className="panel"><div className="section-title compact"><div><span className="eyebrow">Distribución</span><h3>Secciones</h3></div></div>{!sections.data?.length?<Empty>Aún no has creado secciones.</Empty>:<div className="list section-list">{sections.data?.map(s=><Link key={s.id} to={`/admin/sections/${s.id}`}><div><b>{s.name}</b><small>{s.type==='GENERAL_ADMISSION'?'Admisión general':'Asientos numerados'} · {s.capacity} lugares</small></div><span className="list-arrow">Configurar →</span></Link>)}</div>}</div></div>
  </section>
}
