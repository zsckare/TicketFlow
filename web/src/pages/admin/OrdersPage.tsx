import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '../../api/ordersApi'
import { Badge, ErrorState, Loading } from '../../components/Ui'
import { formatDate, formatMoney } from '../../lib/format'
export function OrdersPage() {
  const qc=useQueryClient(); const q=useQuery({queryKey:['orders'],queryFn:ordersApi.getAll})
  const refresh=()=>qc.invalidateQueries({queryKey:['orders']})
  const confirm=useMutation({mutationFn:ordersApi.confirm,onSuccess:()=>void refresh()})
  const cancel=useMutation({mutationFn:ordersApi.cancel,onSuccess:()=>void refresh()})
  if(q.isPending)return <Loading/>; if(q.isError)return <ErrorState error={q.error}/>
  return <section><div className="section-title"><h2>Órdenes</h2><span>{q.data.length}</span></div>
    <div className="table-wrap"><table><thead><tr><th>Orden</th><th>Importe</th><th>Estado</th><th>Creada</th><th></th></tr></thead><tbody>{q.data.map(o=><tr key={o.id}>
      <td><code>{o.id}</code></td><td>{formatMoney(o.amount,o.currency)}</td><td><Badge tone={o.status.toLowerCase()}>{o.status}</Badge></td><td>{formatDate(o.createdAt)}</td>
      <td>{o.status==='RESERVED'&&<div className="inline-actions"><button onClick={()=>confirm.mutate(o.id)}>Confirmar</button><button onClick={()=>cancel.mutate(o.id)}>Cancelar</button></div>}</td>
    </tr>)}</tbody></table></div>
  </section>
}
