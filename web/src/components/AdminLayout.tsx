import { NavLink, Outlet } from 'react-router-dom'
export function AdminLayout() {
  return <main className="page">
    <div className="admin-heading">
      <div><span className="eyebrow">TicketFlow</span><h1>Administración</h1></div>
      <NavLink className="button secondary" to="/events">Ver sitio público</NavLink>
    </div>
    <nav className="admin-nav">
      <NavLink end to="/admin">Dashboard</NavLink>
      <NavLink to="/admin/venues">Venues</NavLink>
      <NavLink to="/admin/events">Eventos</NavLink>
      <NavLink to="/admin/inventory">Inventario</NavLink>
      <NavLink to="/admin/orders">Órdenes</NavLink>
    </nav>
    <Outlet />
  </main>
}
