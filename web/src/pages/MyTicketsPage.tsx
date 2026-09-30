import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { myTicketsApi } from '../api/myTicketsApi'
import { Badge, Empty, ErrorState, Loading } from '../components/Ui'
import { formatDateInTimeZone } from '../lib/format'
import { commerceApi } from '../api/commerceApi'

export function MyTicketsPage() {
  const qc=useQueryClient()
  const transfers=useQuery({queryKey:['ticket-transfers'],queryFn:commerceApi.transfers})
  const accept=useMutation({mutationFn:commerceApi.acceptTransfer,onSuccess:async()=>{await Promise.all([qc.invalidateQueries({queryKey:['ticket-transfers']}),qc.invalidateQueries({queryKey:['my-tickets']})])}})
  const q = useQuery({ queryKey: ['my-tickets'], queryFn: myTicketsApi.getMine })
  const [downloading, setDownloading] = useState<string | null>(null)
  const [downloadError, setDownloadError] = useState<unknown>(null)
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  const download = async (id: string) => { setDownloading(id); setDownloadError(null); try { await myTicketsApi.downloadPdf(id) } catch (e) { setDownloadError(e) } finally { setDownloading(null) } }
  return <main className="page account-page">
    <div className="page-heading"><div><span className="eyebrow">Acceso digital</span><h1>Mis boletos</h1><p>Presenta el QR desde tu teléfono o descarga una copia PDF.</p></div><span className="count-label">{q.data.length} boletos</span></div>
    {downloadError instanceof Error && (
      <ErrorState error={downloadError} />
    )}
    {transfers.data?.some(t=>t.status==='PENDING') && <section className="panel"><h2>Transferencias pendientes</h2>{transfers.data.filter(t=>t.status==='PENDING').map(t=><div className="list-row" key={t.id}><span><b>Boleto #{t.ticketId.slice(0,8).toUpperCase()}</b><small>Enviado a {t.recipientEmail}</small></span><button className="button primary" onClick={()=>accept.mutate(t.transferToken)}>Aceptar</button></div>)}</section>}
    {!q.data.length ? <Empty>Todavía no tienes boletos emitidos.</Empty> : <div className="ticket-grid">{q.data.map(ticket => <article className={`digital-ticket ticket-${ticket.status.toLowerCase()}`} key={ticket.id}>
      <div className="ticket-accent" /><div className="ticket-content">
        <div className="ticket-top"><div><span className="eyebrow">TicketFlow Pass</span><h3>{ticket.eventName ?? 'Evento'}</h3><p className="muted">{ticket.venueName ?? 'Venue por confirmar'}</p>{(ticket.venueAddress || ticket.venueCity) && <small>{[ticket.venueAddress, ticket.venueCity].filter(Boolean).join(' · ')}</small>}</div><Badge tone={ticket.status.toLowerCase()}>{ticket.status}</Badge></div>
        <div className="ticket-details"><span><small>FECHA</small><b>{ticket.eventStartsAt ? formatDateInTimeZone(ticket.eventStartsAt, ticket.venueTimezone) : '—'}</b></span><span><small>SECCIÓN</small><b>{ticket.sectionName ?? 'General'}</b></span><span><small>{ticket.seatLabel ? 'ASIENTO' : 'ACCESO'}</small><b>{ticket.seatLabel ?? 'Admisión general'}</b></span><span><small>ORDEN</small><code>#{ticket.orderId.slice(0, 8).toUpperCase()}</code></span></div>
        {ticket.status === 'ISSUED' && ticket.qrDataUrl && <div className="ticket-qr"><div className="ticket-qr-image"><img src={ticket.qrDataUrl} alt={`QR de acceso para ${ticket.eventName ?? 'el evento'}`} /></div><div className="ticket-qr-copy"><small>CÓDIGO DE ACCESO</small><strong>Presenta este QR en la entrada</strong><p>Este código es único. No lo compartas con otras personas.</p></div></div>}
        {ticket.checkedInAt && <div className="ticket-used">✓ Utilizado {formatDateInTimeZone(ticket.checkedInAt, ticket.venueTimezone)}</div>}
        {ticket.status === 'CANCELLED' && <div className="alert info">Este boleto fue cancelado y ya no permite acceso.</div>}
        <div className="inline-actions"><Link className="button primary" to={`/tickets/${ticket.id}`}>Ver boleto</Link><button className="button secondary" disabled={downloading === ticket.id} onClick={() => void download(ticket.id)}>{downloading === ticket.id ? 'Generando PDF…' : 'Descargar PDF'}</button></div>
      </div>
    </article>)}</div>}
  </main>
}
