import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { apiRequest, json } from '../../api/httpClient'
import { ErrorState } from '../../components/Ui'
import type { IssuedTicketResponse } from '../../types/ticketsOwned'
export function CheckInPage() {
  const [token,setToken]=useState(''); const m=useMutation({mutationFn:(admissionToken:string)=>apiRequest<IssuedTicketResponse>('/orders/tickets/check-in',json({admissionToken})),onSuccess:()=>setToken('')})
  return <section className="checkin-layout"><div className="panel checkin-card"><div className="checkin-icon">✓</div><span className="eyebrow">Control de acceso</span><h2>Validar boleto</h2><p className="muted">Escanea o pega el código de acceso del asistente. Cada boleto puede utilizarse una sola vez.</p><div className="stack"><input className="token-input" value={token} onChange={e=>setToken(e.target.value)} placeholder="Código de acceso" autoFocus/><button className="button primary button-lg" disabled={!token||m.isPending} onClick={()=>m.mutate(token)}>{m.isPending?'Validando…':'Validar entrada'}</button></div>{m.data&&<div className="alert success"><b>Entrada válida</b><span>El boleto fue marcado como utilizado correctamente.</span></div>}{m.error&&<ErrorState error={m.error}/>}</div><aside className="panel checkin-help"><h3>Proceso de acceso</h3><ol><li>Solicita el código del boleto.</li><li>Escanéalo o pégalo en el campo.</li><li>Confirma que el sistema muestre “Entrada válida”.</li><li>Los códigos usados serán rechazados automáticamente.</li></ol></aside></section>
}
