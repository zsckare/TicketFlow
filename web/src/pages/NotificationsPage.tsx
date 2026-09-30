import { useQuery } from '@tanstack/react-query'
import { notificationsApi } from '../api/notificationsApi'
import { Badge, Empty, ErrorState, Loading } from '../components/Ui'
import { formatDate } from '../lib/format'
export function NotificationsPage() {
  const q = useQuery({ queryKey: ['notifications'], queryFn: notificationsApi.mine, refetchInterval: 15000 })
  if (q.isPending) return <Loading />
  if (q.isError) return <ErrorState error={q.error} />
  return <main className="page account-page"><div className="page-heading"><div><span className="eyebrow">Actividad</span><h1>Notificaciones</h1><p>Actualizaciones sobre tus órdenes, pagos y boletos.</p></div><span className="count-label">{q.data.length}</span></div>
    {!q.data.length ? <Empty>No tienes notificaciones nuevas.</Empty> : <div className="notification-list">{q.data.map(n => <article className="panel notification" key={n.id}><div className="notification-mark">✓</div><div className="notification-copy"><span className="eyebrow">{n.type}</span><h3>{n.subject}</h3><p>{n.body}</p><small>{formatDate(n.createdAt)}</small></div><Badge tone={n.status.toLowerCase()}>{n.status}</Badge></article>)}</div>}
  </main>
}
