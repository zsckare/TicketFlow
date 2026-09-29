import { Link } from 'react-router-dom'
import { useEvents } from '../features/events/eventQueries'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-MX', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function EventsPage() {
  const eventsQuery = useEvents()

  if (eventsQuery.isPending) return <div className="state-message">Cargando eventos…</div>
  if (eventsQuery.isError) {
    return <div className="state-message state-message--error">No fue posible cargar los eventos: {eventsQuery.error.message}</div>
  }

  const publishedEvents = eventsQuery.data.filter((event) => event.status === 'PUBLISHED')

  return (
    <main className="page">
      <section className="hero">
        <span className="eyebrow">TicketFlow</span>
        <h1>Encuentra tu próximo evento</h1>
        <p>Explora eventos, selecciona tu asiento y completa tu reservación.</p>
      </section>

      <section>
        <div className="section-heading">
          <div><span className="eyebrow">Cartelera</span><h2>Próximos eventos</h2></div>
          <span className="counter">{publishedEvents.length} eventos</span>
        </div>

        {publishedEvents.length === 0 ? (
          <div className="empty-card">No hay eventos publicados por el momento.</div>
        ) : (
          <div className="event-grid">
            {publishedEvents.map((event) => (
              <article className="event-card" key={event.id}>
                <div className="event-card__art"><span>LIVE</span></div>
                <div className="event-card__body">
                  <span className="status-pill">Publicado</span>
                  <h3>{event.name}</h3>
                  {event.description && <p>{event.description}</p>}
                  <div className="event-card__date">{formatDate(event.startsAt)}</div>
                  <Link className="button button--primary" to={`/events/${event.id}`}>Ver boletos</Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}
