import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '../api/ordersApi'
import { Badge, Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate, formatMoney } from '../lib/format'

export function MyOrdersPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['my-orders'], queryFn: ordersApi.getAll })
  const refresh = async () => { await qc.invalidateQueries({ queryKey: ['my-orders'] }); await qc.invalidateQueries({ queryKey: ['inventory'] }) }
  const confirm = useMutation({ mutationFn: ordersApi.confirm, onSuccess: refresh })
  const cancel = useMutation({ mutationFn: ordersApi.cancel, onSuccess: refresh })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  const error = confirm.error ?? cancel.error
  return <main className="page account-page">
    <div className="page-heading"><div><span className="eyebrow">Tu cuenta</span><h1>Mis órdenes</h1><p>Consulta tus compras y continúa cualquier reservación pendiente.</p></div><span className="count-label">{q.data.length} órdenes</span></div>
    {error && <ErrorState error={error} />}
    {!q.data.length ? <Empty>Aún no has realizado ninguna compra.</Empty> : <div className="order-cards">{q.data.map(order => {
      const processing = (confirm.isPending && confirm.variables === order.id) || (cancel.isPending && cancel.variables === order.id)
      return <article className="panel order-card" key={order.id}>
        <div className="order-main"><div className="order-title"><div><small>ORDEN</small><code>#{order.id.slice(0, 8).toUpperCase()}</code></div><Badge tone={order.status.toLowerCase()}>{order.status}</Badge></div><div className="order-meta"><span>{order.items?.length ?? 1} {(order.items?.length ?? 1) === 1 ? 'boleto' : 'boletos'}</span><span>Creada {formatDate(order.createdAt)}</span></div></div>
        <div className="order-amount"><small>TOTAL</small><strong>{formatMoney(order.amount, order.currency)}</strong></div>
        {order.status === 'RESERVED' && <div className="order-actions"><button className="button primary" disabled={processing} onClick={() => confirm.mutate(order.id)}>Confirmar compra</button><button className="button secondary" disabled={processing} onClick={() => cancel.mutate(order.id)}>Cancelar</button></div>}
        {order.status === 'CONFIRMED' && <div className="order-note success-text">✓ Compra completada</div>}
        {order.status === 'CANCELLED' && <div className="order-note muted">Reservación cancelada</div>}
      </article>
    })}</div>}
  </main>
}
