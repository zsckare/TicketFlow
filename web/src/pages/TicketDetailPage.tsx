import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { myTicketsApi } from '../api/myTicketsApi'
import { Badge, ErrorState, Loading } from '../components/Ui'
import { formatDateInTimeZone } from '../lib/format'
import { commerceApi } from '../api/commerceApi'

export function TicketDetailPage() {
  const { ticketId } = useParams()
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState<unknown>(null)
  const [recipientEmail,setRecipientEmail]=useState('')
  const qc=useQueryClient()
  const q = useQuery({
    queryKey: ['ticket', ticketId],
    queryFn: () => myTicketsApi.get(ticketId!),
    enabled: Boolean(ticketId),
  })

  if (!ticketId) return <main className="page"><ErrorState error={new Error('Boleto inválido')} /></main>
  if (q.isPending) return <Loading text="Cargando boleto…" />
  if (q.isError) return <ErrorState error={q.error} />

  const ticket = q.data
  const transfer=useMutation({mutationFn:()=>commerceApi.transferTicket(ticket.id,recipientEmail),onSuccess:async()=>{setRecipientEmail('');await qc.invalidateQueries({queryKey:['my-tickets']})}})
  const refund=useMutation({mutationFn:()=>commerceApi.refundTicket(ticket.orderId,ticket.id,'Customer requested ticket refund'),onSuccess:async()=>{await qc.invalidateQueries({queryKey:['ticket',ticket.id]});await qc.invalidateQueries({queryKey:['my-tickets']})}})

  const download = async () => {
    setDownloading(true)
    setDownloadError(null)
    try { await myTicketsApi.downloadPdf(ticket.id) }
    catch (error) { setDownloadError(error) }
    finally { setDownloading(false) }
  }

  return <main className="page account-page ticket-detail-page">
    <div className="page-heading">
      <div><span className="eyebrow">TicketFlow Pass</span><h1>{ticket.eventName ?? 'Boleto'}</h1><p>Consulta tu acceso y conserva una copia PDF para el evento.</p></div>
      <Badge tone={ticket.status.toLowerCase()}>{ticket.status}</Badge>
    </div>

    {downloadError instanceof Error && <ErrorState error={downloadError} />}

    <section className={`digital-ticket ticket-${ticket.status.toLowerCase()}`}>
      <div className="ticket-accent" />
      <div className="ticket-content">
        <div className="ticket-top"><div><h2>{ticket.eventName ?? 'Evento'}</h2><p className="muted">{ticket.venueName ?? 'Venue por confirmar'}</p>{(ticket.venueAddress || ticket.venueCity) && <small>{[ticket.venueAddress, ticket.venueCity].filter(Boolean).join(' · ')}</small>}</div></div>
        <div className="ticket-details">
          <span><small>FECHA</small><b>{ticket.eventStartsAt ? formatDateInTimeZone(ticket.eventStartsAt, ticket.venueTimezone) : '—'}</b></span>
          <span><small>SECCIÓN</small><b>{ticket.sectionName ?? 'General'}</b></span>
          <span><small>{ticket.seatLabel ? 'ASIENTO' : 'ACCESO'}</small><b>{ticket.seatLabel ?? 'Admisión general'}</b></span>
          <span><small>ORDEN</small><code>#{ticket.orderId.slice(0, 8).toUpperCase()}</code></span>
        </div>

        {ticket.status === 'ISSUED' && ticket.qrDataUrl && <div className="ticket-qr"><div className="ticket-qr-image"><img src={ticket.qrDataUrl} alt="QR de acceso" /></div><div className="ticket-qr-copy"><small>CÓDIGO DE ACCESO</small><strong>Presenta este QR en la entrada</strong><p>Este código es único. Evita compartir capturas con otras personas.</p></div></div>}
        {ticket.status === 'USED' && <div className="alert success"><b>✓ Boleto utilizado</b><span>{ticket.checkedInAt ? `Check-in: ${formatDateInTimeZone(ticket.checkedInAt, ticket.venueTimezone)}` : 'El acceso ya fue registrado.'}</span></div>}
        {ticket.status === 'CANCELLED' && <div className="alert info"><b>Boleto cancelado</b><span>Este boleto se conserva como historial, pero ya no permite acceso.</span></div>}

        <div className="ticket-audit"><span><small>BOLETO</small><code>#{ticket.id.slice(0, 8).toUpperCase()}</code></span><span><small>EMITIDO</small><b>{formatDateInTimeZone(ticket.issuedAt, ticket.venueTimezone)}</b></span></div>
        {ticket.status === 'ISSUED' && <div className="panel"><h3>Administrar boleto</h3><div className="form-grid"><input type="email" placeholder="Email del destinatario" value={recipientEmail} onChange={e=>setRecipientEmail(e.target.value)}/><button className="button secondary" disabled={!recipientEmail||transfer.isPending} onClick={()=>transfer.mutate()}>Transferir boleto</button><button className="button secondary" disabled={refund.isPending} onClick={()=>{if(window.confirm('¿Reembolsar y cancelar únicamente este boleto?'))refund.mutate()}}>Reembolsar este boleto</button></div>{transfer.isSuccess&&<div className="alert success">Transferencia creada. El destinatario puede aceptarla con su cuenta.</div>}{(transfer.error||refund.error) instanceof Error&&<ErrorState error={(transfer.error||refund.error) as Error}/>}</div>}
        <div className="inline-actions"><button className="button primary" disabled={downloading} onClick={() => void download()}>{downloading ? 'Generando PDF…' : 'Descargar PDF'}</button><Link className="button secondary" to="/tickets">Volver a mis boletos</Link></div>
      </div>
    </section>
  </main>
}
