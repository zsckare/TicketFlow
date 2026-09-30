import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminUsersApi } from '../../api/adminUsersApi'
import { Badge, ErrorState, Loading } from '../../components/Ui'
import type { UserRole } from '../../types/auth'

export function UsersPage(){
 const qc=useQueryClient(); const q=useQuery({queryKey:['admin-users'],queryFn:adminUsersApi.list})
 const [search,setSearch]=useState(''); const [email,setEmail]=useState(''); const [firstName,setFirst]=useState(''); const [lastName,setLast]=useState(''); const [password,setPassword]=useState(''); const [role,setRole]=useState<Exclude<UserRole,'CUSTOMER'>>('STAFF')
 const refresh=()=>qc.invalidateQueries({queryKey:['admin-users']})
 const create=useMutation({mutationFn:adminUsersApi.create,onSuccess:()=>{setEmail('');setFirst('');setLast('');setPassword('');void refresh()}})
 const status=useMutation({mutationFn:({id,status}:{id:string;status:'ACTIVE'|'DISABLED'})=>adminUsersApi.status(id,status),onSuccess:()=>void refresh()})
 const roleMut=useMutation({mutationFn:({id,role}:{id:string;role:'STAFF'|'ADMIN'})=>adminUsersApi.role(id,role),onSuccess:()=>void refresh()})
 if(q.isPending)return <Loading/>; if(q.isError)return <ErrorState error={q.error}/>
 const users=q.data.filter(u=>`${u.firstName} ${u.lastName} ${u.email} ${u.role}`.toLowerCase().includes(search.toLowerCase()))
 const submit=(e:FormEvent)=>{e.preventDefault();create.mutate({email,firstName,lastName,password,role})}
 return <section><div className="section-title"><div><span className="eyebrow">Acceso interno</span><h2>Usuarios de plataforma</h2><p className="muted">Crea staff y administradores. El registro público siempre crea clientes.</p></div><span className="count-label">{q.data.length} usuarios</span></div>
 <div className="admin-grid"><div className="panel"><h3>Nuevo usuario interno</h3><form className="form" onSubmit={submit}><label>Nombre<input required value={firstName} onChange={e=>setFirst(e.target.value)}/></label><label>Apellido<input required value={lastName} onChange={e=>setLast(e.target.value)}/></label><label>Email<input type="email" required value={email} onChange={e=>setEmail(e.target.value)}/></label><label>Contraseña temporal<input type="password" minLength={10} required value={password} onChange={e=>setPassword(e.target.value)}/></label><label>Rol<select value={role} onChange={e=>setRole(e.target.value as 'STAFF'|'ADMIN')}><option value="STAFF">Staff</option><option value="ADMIN">Administrador</option></select></label><button className="button primary" disabled={create.isPending}>Crear usuario</button>{create.error&&<ErrorState error={create.error}/>}</form></div>
 <div className="panel"><div className="section-title compact"><h3>Directorio</h3><input placeholder="Buscar usuario…" value={search} onChange={e=>setSearch(e.target.value)}/></div><div className="table-wrap"><table><thead><tr><th>Usuario</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{users.map(u=><tr key={u.id}><td><div className="table-primary"><b>{u.firstName} {u.lastName}</b><small>{u.email}</small></div></td><td><Badge tone={u.role.toLowerCase()}>{u.role}</Badge></td><td><Badge tone={(u.status??'ACTIVE').toLowerCase()}>{u.status??'ACTIVE'}</Badge></td><td><div className="inline-actions">{u.role!=='CUSTOMER'&&<select value={u.role} onChange={e=>roleMut.mutate({id:u.id,role:e.target.value as 'STAFF'|'ADMIN'})}><option value="STAFF">STAFF</option><option value="ADMIN">ADMIN</option></select>}<button onClick={()=>status.mutate({id:u.id,status:u.status==='DISABLED'?'ACTIVE':'DISABLED'})}>{u.status==='DISABLED'?'Activar':'Desactivar'}</button>{u.role!=='CUSTOMER'&&<button onClick={()=>{const p=window.prompt('Nueva contraseña temporal (mínimo 10 caracteres)');if(p) adminUsersApi.resetPassword(u.id,p).catch(()=>{})}}>Reset password</button>}</div></td></tr>)}</tbody></table></div></div></div></section>
}
