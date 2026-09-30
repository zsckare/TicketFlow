import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { adminUsersApi } from '../../api/adminUsersApi'
import { myTicketsApi } from '../../api/myTicketsApi'
import { Badge, Empty, ErrorState, Loading } from '../../components/Ui'
import { formatDateInTimeZone } from '../../lib/format'

export function TicketsPage() {
  const [search, setSearch] = useState('')
  const tickets = useQuery({ queryKey: ['admin-tickets'], queryFn: () => myTicketsApi.searchAdmin() })
  const users = useQuery({ queryKey: ['admin-users'], queryFn: adminUsersApi.list })
  const usersById = useMemo(() => new Map((users.data ?? []).map(user => [user.id, user])), [users.data])

  if (tickets.isPending) return <Loading />
  if (tickets.isError) return <ErrorState error={tickets.error} />

  const term = search.trim().toLowerCase()
  const filtered = tickets.data.filter(ticket => {
    const buyer = usersById.get(ticket.userId)
    return [ticket.id, ticket.orderId, ticket.eventName, ticket.venueName, ticket.sectionName, ticket.seatLabel, ticket.status, buyer?.email, buyer?.firstName, buyer?.lastName]
      .filter(Boolean).some(value => String(value).toLowerCase().includes(term))
  })
  const issued = tickets.data.filter(ticket => ticket.status === 'ISSUED').length
  const used = tickets.data.filter(ticket => ticket.status === 'USED').length
  const cancelled = tickets.data.filter(ticket => ticket.status === 'CANCELLED').length

  return <section>
    <div className="section-title"><div><span className="eyebrow">Admisiones</span><h2>Boletos</h2><p className="muted">Consulta accesos emitidos, compradores y trazabilidad de check-in.</p></div><span className="count-label">{tickets.data.length} boletos</span></div>
    <div className="compact-stats"><div><small>EMITIDOS</small><b>{issued}</b></div><div><small>UTILIZADOS</small><b>{used}</b></div><div><small>CANCELADOS</small><b>{cancelled}</b></div></div>
    <div className="panel"><div className="section-title compact"><h3>Buscar boletos</h3><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Ticket, orden, comprador, evento, asiento…" /></div>
      {!filtered.length ? <Empty>No encontramos boletos con esos criterios.</Empty> : <div className="table-wrap"><table><thead><tr><th>Boleto</th><th>Evento</th><th>Comprador</th><th>Ubicación</th><th>Estado</th><th>Check-in</th><th /></tr></thead><tbody>{filtered.map(ticket => {
        const buyer = usersById.get(ticket.userId)
        const operator = ticket.checkedInByUserId ? usersById.get(ticket.checkedInByUserId) : undefined
        return <tr key={ticket.id}>
          <td><div className="table-primary"><b>#{ticket.id.slice(0,8).toUpperCase()}</b><small>Orden #{ticket.orderId.slice(0,8).toUpperCase()}</small></div></td>
          <td><div className="table-primary"><b>{ticket.eventName ?? 'Evento'}</b><small>{ticket.venueName ?? '—'}</small></div></td>
          <td><div className="table-primary"><b>{buyer ? `${buyer.firstName ?? ''} ${buyer.lastName ?? ''}`.trim() || buyer.email : `Usuario ${ticket.userId.slice(0,8)}`}</b><small>{buyer?.email ?? ticket.userId}</small></div></td>
          <td>{ticket.seatLabel ? `${ticket.sectionName ?? 'Sección'} · ${ticket.seatLabel}` : ticket.sectionName ?? 'General'}</td>
          <td><Badge tone={ticket.status.toLowerCase()}>{ticket.status}</Badge></td>
          <td>{ticket.checkedInAt ? <div className="table-primary"><b>{formatDateInTimeZone(ticket.checkedInAt, ticket.venueTimezone)}</b><small>{operator ? `${operator.firstName ?? ''} ${operator.lastName ?? ''}`.trim() || operator.email : ticket.checkedInByUserId ? `Staff ${ticket.checkedInByUserId.slice(0,8)}` : 'Staff'}</small></div> : <span className="muted">—</span>}</td>
          <td><Link className="button secondary small" to={`/tickets/${ticket.id}`}>Ver</Link></td>
        </tr>
      })}</tbody></table></div>}
    </div>
  </section>
}
