import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'
import { Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate } from '../lib/format'
export function EventsPage() {
  const q = useQuery({ queryKey: ['events'], queryFn: eventsApi.getEvents })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  const events = q.data.filter(e => e.status === 'PUBLISHED')
  return <main className="page">
    <section className="hero"><span className="eyebrow">TicketFlow</span><h1>Vive el evento.</h1><p>Selecciona tu lugar y reserva tu boleto en segundos.</p></section>
    <div className="section-title"><h2>Próximos eventos</h2><span>{events.length}</span></div>
    {!events.length ? <Empty>No hay eventos publicados.</Empty> :
      <div className="cards">{events.map(e => <article className="event-card" key={e.id}>
        <div className="poster"><span>LIVE</span></div>
        <div className="card-body"><span className="eyebrow">{formatDate(e.startsAt)}</span><h2>{e.name}</h2>
          <p>{e.description}</p><Link className="button primary" to={`/events/${e.id}`}>Ver boletos</Link></div>
      </article>)}</div>}
  </main>
}
