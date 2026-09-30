import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
export function LoginPage() {
  const [email,setEmail]=useState(''); const [password,setPassword]=useState(''); const [error,setError]=useState(''); const [busy,setBusy]=useState(false)
  const {login}=useAuth(); const nav=useNavigate(); const loc=useLocation()
  async function submit(e:FormEvent){e.preventDefault();setBusy(true);setError('');try{await login({email,password});nav((loc.state as {from?:string}|null)?.from??'/events',{replace:true})}catch(e){setError(e instanceof Error?e.message:'No fue posible iniciar sesión')}finally{setBusy(false)}}
  return <main className="auth-page"><section className="panel auth-card"><div className="auth-brand"><span>TF</span></div><span className="eyebrow">Bienvenido de vuelta</span><h1>Inicia sesión</h1><p className="muted">Accede a tus órdenes, boletos y notificaciones desde un solo lugar.</p><form className="form" onSubmit={submit}><label>Correo electrónico<input type="email" autoComplete="email" placeholder="tu@email.com" required value={email} onChange={e=>setEmail(e.target.value)}/></label><label>Contraseña<input type="password" autoComplete="current-password" placeholder="••••••••" required value={password} onChange={e=>setPassword(e.target.value)}/></label><button className="button primary full button-lg" disabled={busy}>{busy?'Entrando…':'Iniciar sesión'}</button>{error&&<div className="alert error">{error}</div>}</form><p className="auth-switch">¿Primera vez en TicketFlow? <Link to="/register">Crear cuenta</Link></p></section></main>
}
