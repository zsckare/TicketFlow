import { useQuery } from '@tanstack/react-query'
import { myTicketsApi } from '../api/myTicketsApi'
import { Badge, Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate } from '../lib/format'

export function MyTicketsPage() {
  const q = useQuery({ queryKey: ['my-tickets'], queryFn: myTicketsApi.getMine })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />

  return <main className="page account-page">
    <div className="page-heading"><div><span className="eyebrow">Acceso digital</span><h1>Mis boletos</h1><p>Todo lo que necesitas para entrar al evento, en un solo lugar.</p></div><span className="count-label">{q.data.length} boletos</span></div>
    {!q.data.length ? <Empty>Todavía no tienes boletos emitidos.</Empty> : <div className="ticket-grid">{q.data.map(ticket => <article className={`digital-ticket ticket-${ticket.status.toLowerCase()}`} key={ticket.id}>
      <div className="ticket-accent" />
      <div className="ticket-content">
        <div className="ticket-top"><div><span className="eyebrow">TicketFlow Pass</span><h3>{ticket.eventName ?? 'Evento'}</h3><p className="muted">{ticket.venueName ?? 'Venue por confirmar'}</p></div><Badge tone={ticket.status.toLowerCase()}>{ticket.status}</Badge></div>
        <div className="ticket-details">
          <span><small>FECHA</small><b>{ticket.eventStartsAt ? formatDate(ticket.eventStartsAt) : '—'}</b></span>
          <span><small>SECCIÓN</small><b>{ticket.sectionName ?? 'General'}</b></span>
          <span><small>{ticket.seatLabel ? 'ASIENTO' : 'ACCESO'}</small><b>{ticket.seatLabel ?? 'Admisión general'}</b></span>
          <span><small>ORDEN</small><code>#{ticket.orderId.slice(0, 8).toUpperCase()}</code></span>
        </div>
        <div className="access-code"><small>CÓDIGO DE ACCESO</small><code>{ticket.admissionToken}</code><p>Presenta este código en la entrada. No lo compartas con otras personas.</p></div>
        {ticket.checkedInAt && <div className="ticket-used">✓ Utilizado {formatDate(ticket.checkedInAt)}</div>}
        {ticket.status === 'CANCELLED' && <div className="alert info">Este boleto fue cancelado y ya no permite acceso.</div>}
      </div>
    </article>)}</div>}
  </main>
}
