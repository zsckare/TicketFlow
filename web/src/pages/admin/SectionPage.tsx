import { useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { Empty, ErrorState, Loading } from '../../components/Ui'

export function SectionPage() {
  const { sectionId = '' } = useParams()
  const qc = useQueryClient()
  const section = useQuery({ queryKey: ['section', sectionId], queryFn: () => eventsApi.getSection(sectionId) })
  const seats = useQuery({ queryKey: ['seats', sectionId], queryFn: () => eventsApi.getSeats(sectionId), enabled: section.data?.type === 'RESERVED_SEATING' })
  const [row, setRow] = useState('A')
  const [number, setNumber] = useState('')
  const moveSeat=useMutation({mutationFn:({id,x,y}:{id:string;x:number;y:number})=>eventsApi.updateSeatMap(id,x,y),onSuccess:()=>qc.invalidateQueries({queryKey:['seats',sectionId]})})
  const create = useMutation({ mutationFn: () => eventsApi.createSeat(sectionId, { row: row.trim().toUpperCase(), number: number.trim() }), onSuccess: () => { setNumber(''); void qc.invalidateQueries({ queryKey: ['seats', sectionId] }) } })

  if (section.isPending) return <Loading />
  if (section.isError) return <ErrorState error={section.error} />
  const isGeneral = section.data.type === 'GENERAL_ADMISSION'

  return <section>
    <div className="section-title"><div><span className="eyebrow">Configuración del venue</span><h2>{section.data.name}</h2><p className="muted">{isGeneral ? 'Admisión general' : 'Asientos numerados'} · capacidad {section.data.capacity}</p></div><span className="count-label">{isGeneral ? `${section.data.capacity} lugares` : `${seats.data?.length ?? 0} asientos`}</span></div>
    {isGeneral ? <div className="panel empty-feature"><div className="feature-icon">◎</div><h3>Sección de admisión general</h3><p>Esta sección no necesita asientos individuales. La cantidad vendible y su precio se administran desde Inventario.</p></div> : <div className="admin-grid">
      <div className="panel"><span className="eyebrow">Agregar localidad</span><h3>Nuevo asiento</h3><p className="muted">Crea cada localidad física usando fila y número.</p><form className="form" onSubmit={(e:FormEvent)=>{e.preventDefault();create.mutate()}}><div className="form-row"><label>Fila<input value={row} onChange={e=>setRow(e.target.value)} required/></label><label>Número<input value={number} onChange={e=>setNumber(e.target.value)} required/></label></div><button className="button primary" disabled={create.isPending}>{create.isPending ? 'Creando…' : 'Crear asiento'}</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
      <div className="panel"><div className="section-title compact"><div><span className="eyebrow">Mapa</span><h3>Asientos creados</h3></div><span className="count-label">{seats.data?.length ?? 0}</span></div>{!seats.data?.length ? <Empty>Aún no hay asientos en esta sección.</Empty> : <><div className="stage admin-stage">ESCENARIO</div><p className="muted">Haz clic en un asiento y después en el mapa para colocarlo.</p><SeatMapEditor seats={seats.data} onMove={(id,x,y)=>moveSeat.mutate({id,x,y})}/></>}</div>
    </div>}
  </section>
}

function SeatMapEditor({seats,onMove}:{seats:import('../../types/events').SeatResponse[];onMove:(id:string,x:number,y:number)=>void}){const [selected,setSelected]=useState<string|null>(null);return <div className="venue-map-editor" onClick={e=>{if(!selected)return;const r=e.currentTarget.getBoundingClientRect();onMove(selected,((e.clientX-r.left)/r.width)*100,((e.clientY-r.top)/r.height)*100)}}>{seats.map(s=><button type="button" key={s.id} className={`map-seat ${selected===s.id?'selected':''}`} style={{left:`${s.mapX??10}%`,top:`${s.mapY??10}%`}} onClick={e=>{e.stopPropagation();setSelected(s.id)}}>{s.row}{s.number}</button>)}</div>}
