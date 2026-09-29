import {  useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ErrorState, Loading } from '../../components/Ui'
export function SectionPage() {
  const {sectionId=''}=useParams(); const qc=useQueryClient()
  const section=useQuery({queryKey:['section',sectionId],queryFn:()=>eventsApi.getSection(sectionId)})
  const seats=useQuery({queryKey:['seats',sectionId],queryFn:()=>eventsApi.getSeats(sectionId)})
  const [row,setRow]=useState('A'); const [number,setNumber]=useState('')
  const create=useMutation({mutationFn:()=>eventsApi.createSeat(sectionId,{row,number}),onSuccess:()=>{setNumber('');void qc.invalidateQueries({queryKey:['seats',sectionId]})}})
  if(section.isPending)return <Loading/>; if(section.isError)return <ErrorState error={section.error}/>
  return <section><div className="section-title"><div><span className="eyebrow">Sección</span><h2>{section.data.name}</h2><p className="muted">{section.data.type}</p></div></div>
    <div className="admin-grid">
      <div className="panel"><h3>Nuevo asiento</h3>{section.data.type==='GENERAL_ADMISSION'?<div className="alert info">General Admission utiliza capacidad, no asientos individuales.</div>:
      <form className="form" onSubmit={(e:FormEvent)=>{e.preventDefault();create.mutate()}}>
        <label>Fila<input value={row} onChange={e=>setRow(e.target.value)} required/></label>
        <label>Número<input value={number} onChange={e=>setNumber(e.target.value)} required/></label>
        <button className="button primary">Crear asiento</button>{create.error&&<ErrorState error={create.error}/>}
      </form>}</div>
      <div className="panel"><h3>Asientos ({seats.data?.length??0})</h3><div className="mini-seat-grid">{seats.data?.map(s=><div key={s.id}><b>{s.row}{s.number}</b><small>{s.id.slice(0,8)}</small></div>)}</div></div>
    </div>
  </section>
}
