import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Html5Qrcode } from 'html5-qrcode'
import { apiRequest, json } from '../../api/httpClient'
import { eventsApi } from '../../api/eventsApi'
import { ErrorState } from '../../components/Ui'
import type { IssuedTicketResponse } from '../../types/ticketsOwned'

const SCANNER_ELEMENT_ID='ticketflow-qr-reader'
type Stats={eventId:string;total:number;checkedIn:number;pending:number;cancelled:number}

export function CheckInPage(){
 const qc=useQueryClient(); const [payload,setPayload]=useState(''); const [eventId,setEventId]=useState(''); const [scanning,setScanning]=useState(false); const [scanError,setScanError]=useState(''); const scanner=useRef<Html5Qrcode|null>(null); const scanLocked=useRef(false)
 const events=useQuery({queryKey:['events'],queryFn:eventsApi.getEvents}); const published=(events.data??[]).filter(e=>e.status==='PUBLISHED')
 const stats=useQuery({queryKey:['checkin-stats',eventId],queryFn:()=>apiRequest<Stats>(`/orders/tickets/check-in/stats/${eventId}`),enabled:!!eventId,refetchInterval:15000})
 const stop=async()=>{const current=scanner.current;scanner.current=null;scanLocked.current=false;if(current){try{if(current.isScanning)await current.stop()}catch{}try{current.clear()}catch{}}setScanning(false)}
 const checkIn=useMutation({mutationFn:(qrPayload:string)=>apiRequest<IssuedTicketResponse>('/orders/tickets/check-in',json({qrPayload,eventId:eventId||null})),onSuccess:()=>{setPayload('');void stop();void qc.invalidateQueries({queryKey:['checkin-stats',eventId]})},onError:()=>{scanLocked.current=false}})
 const submit=(v:string)=>{const value=v.trim();if(value&&!checkIn.isPending&&eventId)checkIn.mutate(value)}
 const start=async()=>{setScanError('');checkIn.reset();if(!eventId){setScanError('Selecciona el evento antes de abrir el scanner.');return}if(!navigator.mediaDevices?.getUserMedia){setScanError('La cámara requiere un navegador compatible y HTTPS.');return}try{setScanning(true);await new Promise<void>(r=>requestAnimationFrame(()=>r()));const instance=new Html5Qrcode(SCANNER_ELEMENT_ID);scanner.current=instance;await instance.start({facingMode:'environment'},{fps:10,qrbox:{width:250,height:250}},text=>{if(text.trim()&&!scanLocked.current){scanLocked.current=true;submit(text)}},()=>{})}catch(e){await stop();setScanError(e instanceof Error?e.message:'No fue posible abrir la cámara')}}
 useEffect(()=>()=>{void stop()},[])
 const selected=published.find(e=>e.id===eventId)
 return <section><div className="section-title"><div><span className="eyebrow">Control de acceso</span><h2>Check-in</h2><p className="muted">Selecciona el evento y valida boletos con feedback inmediato.</p></div></div>
 <div className="compact-stats"><div><small>EMITIDOS</small><b>{stats.data?.total??'—'}</b></div><div><small>INGRESARON</small><b>{stats.data?.checkedIn??'—'}</b></div><div><small>PENDIENTES</small><b>{stats.data?.pending??'—'}</b></div><div><small>ASISTENCIA</small><b>{stats.data?.total?`${Math.round(stats.data.checkedIn/stats.data.total*100)}%`:'—'}</b></div></div>
 <div className="checkin-layout"><div className="panel checkin-card"><label>Evento<select value={eventId} onChange={e=>{setEventId(e.target.value);checkIn.reset()}}><option value="">Selecciona un evento…</option>{published.map(e=><option key={e.id} value={e.id}>{e.name}</option>)}</select></label><h3>{selected?.name??'Scanner de acceso'}</h3>
 {scanning?<div className="scanner"><div id={SCANNER_ELEMENT_ID} className="qr-reader"/><button className="button secondary full" onClick={()=>void stop()}>Cerrar cámara</button></div>:<button className="button primary full button-lg" disabled={!eventId} onClick={()=>void start()}>Abrir scanner QR</button>}
 <div className="manual-checkin"><span>o pega el payload firmado</span><div className="stack"><input className="token-input" value={payload} onChange={e=>setPayload(e.target.value)} placeholder="ticketflow:v1:..."/><button className="button secondary full" disabled={!eventId||!payload.trim()||checkIn.isPending} onClick={()=>submit(payload)}>{checkIn.isPending?'Validando…':'Validar entrada'}</button></div></div>
 {scanError&&<div className="alert info">{scanError}</div>}{checkIn.data&&<div className="alert success"><b>✓ ENTRADA VÁLIDA</b><span>{checkIn.data.eventName??'Boleto'}{checkIn.data.sectionName?` · ${checkIn.data.sectionName}`:''}{checkIn.data.seatLabel?` · Asiento ${checkIn.data.seatLabel}`:''}</span><small>Check-in {checkIn.data.checkedInAt??''}</small></div>}{checkIn.error&&<ErrorState error={checkIn.error}/>}</div>
 <aside className="panel checkin-help"><h3>Operación en puerta</h3><p className="muted">El evento seleccionado también evita aceptar por error un QR válido de otro evento.</p><ol><li>Selecciona el evento.</li><li>Escanea el QR.</li><li>TicketFlow valida firma, evento y estado.</li><li>El boleto cambia atómicamente de ISSUED a USED.</li></ol></aside></div></section>
}
