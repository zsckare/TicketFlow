import { NavLink, Outlet } from 'react-router-dom'
export function AdminLayout() {
  return <main className="page admin-page">
    <div className="admin-heading"><div><span className="eyebrow">Centro de operaciones</span><h1>Administración</h1><p className="muted">Gestiona eventos, inventario y operaciones desde un solo lugar.</p></div><NavLink className="button secondary" to="/events">Ver sitio público ↗</NavLink></div>
    <nav className="admin-nav" aria-label="Administración"><NavLink end to="/admin">Dashboard</NavLink><NavLink to="/admin/venues">Venues</NavLink><NavLink to="/admin/events">Eventos</NavLink><NavLink to="/admin/inventory">Inventario</NavLink><NavLink to="/admin/orders">Órdenes</NavLink><NavLink to="/admin/check-in">Check-in</NavLink></nav>
    <Outlet />
  </main>
}
