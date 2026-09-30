import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'
import { Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate } from '../lib/format'

export function EventsPage() {
  const q = useQuery({ queryKey: ['events'], queryFn: eventsApi.getEvents })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  const events = q.data.filter(event => event.status === 'PUBLISHED')
  return <main className="page">
    <section className="hero"><span className="eyebrow">Experiencias que se viven</span><h1>Tu próxima noche empieza aquí.</h1><p>Descubre eventos, elige tu lugar y asegura tus boletos desde una experiencia simple y rápida.</p><div className="hero-trust"><span>✓ Selección en tiempo real</span><span>✓ Compra protegida</span><span>✓ Boletos digitales</span></div></section>
    <div className="section-title"><div><span className="eyebrow">Cartelera</span><h2>Próximos eventos</h2></div><span className="count-label">{events.length} {events.length === 1 ? 'evento' : 'eventos'}</span></div>
    {!events.length ? <Empty>No hay eventos publicados por el momento.</Empty> : <div className="cards">{events.map((event, index) => <article className="event-card" key={event.id}>
      <div className={`poster poster-${(index % 3) + 1}`}><span className="poster-label">EN VIVO</span><strong>{event.name.slice(0, 2).toUpperCase()}</strong></div>
      <div className="card-body"><span className="eyebrow">{formatDate(event.startsAt)}</span><h2>{event.name}</h2><p>{event.description || 'Una experiencia para disfrutar en vivo.'}</p><Link className="button primary full" to={`/events/${event.id}`}>Explorar boletos <span>→</span></Link></div>
    </article>)}</div>}
  </main>
}
