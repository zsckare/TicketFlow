import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '../../api/ordersApi'
import { Badge, Empty, ErrorState, Loading } from '../../components/Ui'
import { formatDate, formatMoney } from '../../lib/format'

export function OrdersPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['orders'], queryFn: ordersApi.getAll })
  const refresh = () => qc.invalidateQueries({ queryKey: ['orders'] })
  const cancel = useMutation({ mutationFn: ordersApi.cancel, onSuccess: () => void refresh() })

  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />

  const confirmed = q.data.filter(order => order.status === 'CONFIRMED').length
  const reserved = q.data.filter(order => order.status === 'RESERVED').length
  const cancelled = q.data.filter(order => order.status === 'CANCELLED').length

  return <section>
    <div className="section-title"><div><span className="eyebrow">Operación</span><h2>Órdenes</h2><p className="muted">Supervisa reservaciones y compras desde una sola vista.</p></div><span className="count-label">{q.data.length} totales</span></div>
    <div className="compact-stats"><div><small>CONFIRMADAS</small><b>{confirmed}</b></div><div><small>RESERVADAS</small><b>{reserved}</b></div><div><small>CANCELADAS</small><b>{cancelled}</b></div></div>
    {cancel.error && <ErrorState error={cancel.error} />}
    {!q.data.length ? <Empty>No hay órdenes todavía.</Empty> : <div className="table-wrap"><table><thead><tr><th>Orden</th><th>Boletos</th><th>Importe</th><th>Estado</th><th>Creada</th><th>Acciones</th></tr></thead><tbody>{q.data.map(order => {
      const busy = cancel.isPending && cancel.variables === order.id
      return <tr key={order.id}>
        <td><div className="table-primary"><b>#{order.id.slice(0,8).toUpperCase()}</b><small>{order.userId ? `Cliente ${order.userId.slice(0,8).toUpperCase()}` : 'Cliente'}</small></div></td>
        <td>{order.items?.length ?? 1}</td>
        <td><b>{formatMoney(order.amount,order.currency)}</b></td>
        <td><Badge tone={order.status.toLowerCase()}>{order.status}</Badge></td>
        <td>{formatDate(order.createdAt)}</td>
        <td>{order.status==='RESERVED' ? <div className="inline-actions"><button disabled={busy} onClick={()=>cancel.mutate(order.id)}>Cancelar reserva</button></div> : <span className="muted">—</span>}</td>
      </tr>
    })}</tbody></table></div>}
  </section>
}
