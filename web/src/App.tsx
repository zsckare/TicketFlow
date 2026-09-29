import { Navigate, Route, Routes } from 'react-router-dom'
import { EventDetailPage } from './pages/EventDetailPage'
import { EventsPage } from './pages/EventsPage'

export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/events">
          <span className="brand__mark">TF</span>
          <span>TicketFlow</span>
        </a>
        <span className="topbar__caption">Tickets made simple</span>
      </header>
      <Routes>
        <Route path="/" element={<Navigate replace to="/events" />} />
        <Route path="/events" element={<EventsPage />} />
        <Route path="/events/:eventId" element={<EventDetailPage />} />
        <Route path="*" element={<Navigate replace to="/events" />} />
      </Routes>
    </div>
  )
}
