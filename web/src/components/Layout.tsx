import { NavLink, Outlet } from 'react-router-dom'
export function Layout() {
  return <div className="shell">
    <header className="topbar">
      <NavLink to="/events" className="brand"><b>TF</b><span>TicketFlow</span></NavLink>
      <nav className="main-nav">
        <NavLink to="/events">Eventos</NavLink>
        <NavLink to="/admin">Admin</NavLink>
      </nav>
    </header>
    <Outlet />
  </div>
}
