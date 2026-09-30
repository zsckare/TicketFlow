import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useQuery } from '@tanstack/react-query'
import { ordersApi } from '../api/ordersApi'

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const cart = useQuery({ queryKey: ['active-order'], queryFn: ordersApi.getActive, enabled: !!user })
  return <div className="shell">
    <header className="topbar">
      <NavLink to="/events" className="brand"><b>TF</b><span>TicketFlow</span></NavLink>
      <nav className="main-nav" aria-label="Navegación principal">
        <NavLink to="/events">Eventos</NavLink>
        {user && <NavLink to="/cart">Carrito{cart.data ? ` (${cart.data.items.length})` : ''}</NavLink>}
        {user && <NavLink to="/orders">Órdenes</NavLink>}
        {user && <NavLink to="/tickets">Boletos</NavLink>}
        {user && <NavLink to="/notifications">Notificaciones</NavLink>}
        {user?.role === 'ADMIN' && <NavLink to="/admin">Administrar</NavLink>}
        {!user ? <NavLink className="nav-cta" to="/login">Iniciar sesión</NavLink> : <button className="nav-button" onClick={() => { logout(); navigate('/events') }}>Salir <span>· {user.firstName ?? user.email}</span></button>}
      </nav>
    </header>
    <Outlet />
  </div>
}
