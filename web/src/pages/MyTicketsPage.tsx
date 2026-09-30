import { useQuery } from '@tanstack/react-query'
import { myTicketsApi } from '../api/myTicketsApi'
import { Badge, Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate } from '../lib/format'

export function MyTicketsPage() {
  const q = useQuery({ queryKey: ['my-tickets'], queryFn: myTicketsApi.getMine })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  return <main className="page account-page"><div className="page-heading"><div><span className="eyebrow">Acceso digital</span><h1>Mis boletos</h1><p>Ten tus accesos listos antes de llegar al evento.</p></div><span className="count-label">{q.data.length} boletos</span></div>
    {!q.data.length ? <Empty>Todavía no tienes boletos emitidos.</Empty> : <div className="ticket-grid">{q.data.map(ticket => <article className="digital-ticket" key={ticket.id}>
      <div className="ticket-accent" /><div className="ticket-content"><div className="ticket-top"><div><span className="eyebrow">TicketFlow Pass</span><h3>{ticket.seatId ? 'Asiento numerado' : 'Admisión general'}</h3></div><Badge tone={ticket.status.toLowerCase()}>{ticket.status}</Badge></div>
      <div className="ticket-details"><span><small>EVENTO</small><code>{ticket.eventId.slice(0, 8).toUpperCase()}</code></span><span><small>ORDEN</small><code>{ticket.orderId.slice(0, 8).toUpperCase()}</code></span><span><small>EMITIDO</small><b>{formatDate(ticket.issuedAt)}</b></span></div>
      <div className="access-code"><small>CÓDIGO DE ACCESO</small><code>{ticket.admissionToken}</code><p>Presenta este código en la entrada. No lo compartas.</p></div>{ticket.checkedInAt && <div className="ticket-used">✓ Utilizado {formatDate(ticket.checkedInAt)}</div>}</div>
    </article>)}</div>}
  </main>
}
