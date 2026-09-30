import { useQuery } from '@tanstack/react-query'

import { notificationsApi } from '../api/notificationsApi'
import {
  Badge,
  Empty,
  ErrorState,
  Loading,
} from '../components/Ui'
import { formatDate } from '../lib/format'

/**
 * Convierte HTML almacenado en notificaciones antiguas a texto plano.
 *
 * Algunas notificaciones históricas fueron creadas utilizando directamente
 * el HTML del correo electrónico como body. No queremos renderizar ese HTML
 * con dangerouslySetInnerHTML, ya que el contenido de email no pertenece
 * directamente a la UI y además introduciría un riesgo innecesario de XSS.
 *
 * Las notificaciones nuevas deberían almacenar texto plano desde backend,
 * pero mantenemos esta función para conservar compatibilidad con registros
 * existentes.
 */
function htmlToPlainText(value: string): string {
  if (!value.includes('<')) {
    return value.trim()
  }

  const document = new DOMParser().parseFromString(
    value,
    'text/html',
  )

  return (document.body.textContent ?? value)
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function NotificationsPage() {
  const notifications = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsApi.mine,
    refetchInterval: 15_000,
  })

  if (notifications.isPending) {
    return <Loading />
  }

  if (notifications.isError) {
    return (
      <ErrorState
        error={notifications.error}
      />
    )
  }

  const data = notifications.data

  return (
    <main className="page account-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            Actividad
          </span>

          <h1>Notificaciones</h1>

          <p className="muted">
            Actualizaciones sobre tus órdenes,
            pagos y boletos.
          </p>
        </div>

        <span className="count-label">
          {data.length}{' '}
          {data.length === 1
            ? 'notificación'
            : 'notificaciones'}
        </span>
      </div>

      {!data.length ? (
        <Empty>
          No tienes notificaciones nuevas.
        </Empty>
      ) : (
        <div
          className="notification-list"
          style={{
            display: 'grid',
            gap: '1rem',
          }}
        >
          {data.map((notification) => (
            <article
              className="panel notification"
              key={notification.id}
              style={{
                display: 'block',
              }}
            >
              {/*
               * Cabecera.
               *
               * El tipo identifica el origen de la actividad
               * y el badge muestra su estado actual.
               */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent:
                    'space-between',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                  marginBottom: '0.75rem',
                }}
              >
                <span className="eyebrow">
                  {notification.type}
                </span>

                <Badge
                  tone={
                    notification.status.toLowerCase()
                  }
                >
                  {notification.status}
                </Badge>
              </div>

              {/*
               * Contenido.
               *
               * Utilizamos todo el ancho disponible para que
               * mensajes largos no compriman el badge ni otros
               * elementos de la tarjeta.
               */}
              <div className="notification-copy">
                <h3
                  style={{
                    marginTop: 0,
                    marginBottom: '0.5rem',
                  }}
                >
                  {notification.subject}
                </h3>

                <p
                  style={{
                    marginTop: 0,
                    whiteSpace: 'pre-wrap',
                    overflowWrap: 'anywhere',
                  }}
                >
                  {htmlToPlainText(
                    notification.body,
                  )}
                </p>

                <div
                  style={{
                    marginTop: '1rem',
                    paddingTop: '0.75rem',
                    borderTop:
                      '1px solid var(--border, rgba(255,255,255,.08))',
                  }}
                >
                  <small className="muted">
                    {formatDate(
                      notification.createdAt,
                    )}
                  </small>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}