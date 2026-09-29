import {  useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ticketsApi } from '../../api/ticketsApi'
import { Badge, ErrorState } from '../../components/Ui'
import { formatMoney } from '../../lib/format'
export function InventoryPage() {
  const qc=useQueryClient(); const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents})
  const [eventId,setEventId]=useState(''); const [seatId,setSeatId]=useState(''); const [price,setPrice]=useState('1500.00'); const [currency,setCurrency]=useState('MXN')
  const inventory=useQuery({queryKey:['inventory',eventId],queryFn:()=>ticketsApi.getEventInventory(eventId),enabled:!!eventId})
  const create=useMutation({mutationFn:ticketsApi.createInventory,onSuccess:()=>void qc.invalidateQueries({queryKey:['inventory',eventId]})})
  return <section><div className="section-title"><h2>Inventario</h2></div><div className="admin-grid">
    <div className="panel"><h3>Crear boleto</h3><form className="form" onSubmit={(e:FormEvent)=>{e.preventDefault();create.mutate({eventId,seatId,price,currency})}}>
      <label>Evento<select required value={eventId} onChange={e=>setEventId(e.target.value)}><option value="">Selecciona…</option>{events.data?.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
      <label>Seat ID<input required value={seatId} onChange={e=>setSeatId(e.target.value)} placeholder="UUID del asiento"/></label>
      <label>Precio<input required value={price} onChange={e=>setPrice(e.target.value)}/></label>
      <label>Moneda<input required maxLength={3} value={currency} onChange={e=>setCurrency(e.target.value.toUpperCase())}/></label>
      <button className="button primary">Crear inventario</button>{create.error&&<ErrorState error={create.error}/>}</form>
      <p className="hint">El backend actual no expone todos los asientos de un venue en una sola consulta; por ahora se usa el Seat ID.</p>
    </div>
    <div className="panel"><h3>Inventario del evento</h3>{inventory.data?.map(i=><div className="inventory-row" key={i.id}><div><b>{formatMoney(i.price,i.currency)}</b><small>{i.seatId}</small></div><Badge tone={i.status.toLowerCase()}>{i.status}</Badge></div>)}</div>
  </div></section>
}
