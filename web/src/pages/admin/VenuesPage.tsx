import {  useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { Empty, ErrorState, Loading } from '../../components/Ui'
export function VenuesPage() {
  const qc=useQueryClient(); const q=useQuery({queryKey:['venues'],queryFn:eventsApi.getVenues})
  const [name,setName]=useState(''); const [address,setAddress]=useState(''); const [city,setCity]=useState('')
  const create=useMutation({mutationFn:eventsApi.createVenue,onSuccess:()=>{setName('');setAddress('');setCity('');void qc.invalidateQueries({queryKey:['venues']})}})
  const submit=(e:FormEvent)=>{e.preventDefault();create.mutate({name,address,city})}
  if(q.isPending)return <Loading/>; if(q.isError)return <ErrorState error={q.error}/>
  return <section><div className="section-title"><h2>Venues</h2></div>
    <div className="admin-grid"><div className="panel"><h3>Nuevo venue</h3><form className="form" onSubmit={submit}>
      <label>Nombre<input value={name} onChange={e=>setName(e.target.value)} required/></label>
      <label>Dirección<input value={address} onChange={e=>setAddress(e.target.value)} required/></label>
      <label>Ciudad<input value={city} onChange={e=>setCity(e.target.value)} required/></label>
      <button className="button primary" disabled={create.isPending}>Crear venue</button>{create.error&&<ErrorState error={create.error}/>}
    </form></div>
    <div className="panel"><h3>Todos los venues</h3>{!q.data.length?<Empty>Sin venues.</Empty>:<div className="list">{q.data.map(v=>
      <Link key={v.id} to={`/admin/venues/${v.id}`}><div><b>{v.name}</b><small>{v.address} · {v.city}</small></div><span>→</span></Link>)}</div>}</div></div>
  </section>
}
