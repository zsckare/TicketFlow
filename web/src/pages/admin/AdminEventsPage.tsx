import { useState } from 'react'
import type { FormEvent } from 'react'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import { eventsApi } from '../../api/eventsApi'
import {
  Badge,
  Empty,
  ErrorState,
  Loading,
} from '../../components/Ui'
import { formatDate } from '../../lib/format'

export function AdminEventsPage() {
  const queryClient = useQueryClient()

  const events = useQuery({
    queryKey: ['events'],
    queryFn: eventsApi.getEvents,
  })

  const venues = useQuery({
    queryKey: ['venues'],
    queryFn: eventsApi.getVenues,
  })

  const [venueId, setVenueId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: eventsApi.createEvent,
    onSuccess: () => {
      setName('')
      setDescription('')
      setStartsAt('')
      setEndsAt('')
      setFormError(null)

      void queryClient.invalidateQueries({
        queryKey: ['events'],
      })
    },
  })

  const publish = useMutation({
    mutationFn: eventsApi.publishEvent,
    onSuccess: () =>
      void queryClient.invalidateQueries({
        queryKey: ['events'],
      }),
  })

  const cancel = useMutation({
    mutationFn: eventsApi.cancelEvent,
    onSuccess: () =>
      void queryClient.invalidateQueries({
        queryKey: ['events'],
      }),
  })

  const remove = useMutation({
    mutationFn: eventsApi.deleteEvent,
    onSuccess: () =>
      void queryClient.invalidateQueries({
        queryKey: ['events'],
      }),
  })

  const clone = useMutation({
    mutationFn: eventsApi.cloneEvent,
    onSuccess: () =>
      void queryClient.invalidateQueries({
        queryKey: ['events'],
      }),
  })

  const edit = useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: string
      body: {
        name: string
        description: string
        startsAt: string
        endsAt: string
      }
    }) => eventsApi.updateEvent(id, body),

    onSuccess: () =>
      void queryClient.invalidateQueries({
        queryKey: ['events'],
      }),
  })

  if (events.isPending) {
    return <Loading />
  }

  if (events.isError) {
    return <ErrorState error={events.error} />
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()

    setFormError(null)

    const startDate = new Date(startsAt)
    const endDate = new Date(endsAt)

    if (Number.isNaN(startDate.getTime())) {
      setFormError('Selecciona una fecha de inicio válida.')
      return
    }

    if (Number.isNaN(endDate.getTime())) {
      setFormError('Selecciona una fecha de fin válida.')
      return
    }

    if (endDate <= startDate) {
      setFormError(
        'La fecha de fin debe ser posterior a la fecha de inicio.',
      )
      return
    }

    create.mutate({
      venueId,
      name,
      description,
      startsAt: startDate.toISOString(),
      endsAt: endDate.toISOString(),
    })
  }

  const actionError =
    publish.error ??
    cancel.error ??
    remove.error

  return (
    <section>
      <div className="section-title">
        <div>
          <span className="eyebrow">
            Programación
          </span>

          <h2>Eventos</h2>

          <p className="muted">
            Crea borradores, prepara su inventario y
            publícalos cuando estén listos.
          </p>
        </div>

        <span className="count-label">
          {events.data.length} eventos
        </span>
      </div>

      {actionError && (
        <ErrorState error={actionError} />
      )}

      {/*
       * El formulario y el listado se muestran verticalmente.
       *
       * De esta manera el formulario aprovecha todo el ancho
       * disponible y los selectores de fecha no quedan comprimidos
       * por el grid general del administrador.
       */}
      <div
        className="admin-events-layout"
        style={{
          display: 'grid',
          gap: '1.5rem',
        }}
      >
        <div className="panel">
          <span className="eyebrow">
            Nuevo
          </span>

          <h3>Crear evento</h3>

          <p className="muted">
            El evento se crea como borrador. Después
            configura su inventario antes de publicarlo.
          </p>

          <form
            className="form"
            onSubmit={submit}
          >
            {/*
             * Los campos generales pueden aprovechar el ancho
             * completo del panel.
             */}
            <label>
              Venue

              <select
                required
                value={venueId}
                onChange={(event) =>
                  setVenueId(event.target.value)
                }
              >
                <option value="">
                  Selecciona un venue…
                </option>

                {venues.data?.map((venue) => (
                  <option
                    key={venue.id}
                    value={venue.id}
                  >
                    {venue.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Nombre

              <input
                placeholder="Nombre del evento"
                required
                value={name}
                onChange={(event) =>
                  setName(event.target.value)
                }
              />
            </label>

            <label>
              Descripción

              <textarea
                placeholder="Describe brevemente la experiencia"
                value={description}
                onChange={(event) =>
                  setDescription(event.target.value)
                }
              />
            </label>

            {/*
             * Inicio y fin permanecen juntos en desktop.
             *
             * Cada control tiene minWidth: 0 para permitir que
             * el navegador calcule correctamente su tamaño dentro
             * del grid sin provocar overflow.
             */}
            <div
              className="event-date-grid"
              style={{
                display: 'grid',
                gridTemplateColumns:
                  'repeat(auto-fit, minmax(280px, 1fr))',
                gap: '1rem',
                width: '100%',
              }}
            >
              <label style={{ minWidth: 0 }}>
                Inicio

                <input
                  required
                  type="datetime-local"
                  value={startsAt}
                  onChange={(event) =>
                    setStartsAt(event.target.value)
                  }
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                  }}
                />
              </label>

              <label style={{ minWidth: 0 }}>
                Fin

                <input
                  required
                  type="datetime-local"
                  value={endsAt}
                  min={startsAt || undefined}
                  onChange={(event) =>
                    setEndsAt(event.target.value)
                  }
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                  }}
                />
              </label>
            </div>

            {formError && (
              <div
                className="error-message"
                role="alert"
              >
                {formError}
              </div>
            )}

            <div>
              <button
                className="button primary"
                disabled={create.isPending}
              >
                {create.isPending
                  ? 'Creando…'
                  : 'Crear borrador'}
              </button>
            </div>

            {create.error && (
              <ErrorState error={create.error} />
            )}
          </form>
        </div>

        <div className="panel">
          <div className="section-title compact">
            <div>
              <span className="eyebrow">
                Calendario
              </span>

              <h3>Todos los eventos</h3>
            </div>
          </div>

          {!events.data.length ? (
            <Empty>
              No hay eventos todavía.
            </Empty>
          ) : (
            <div className="list event-admin-list">
              {events.data.map((event) => (
                <div
                  className="list-row"
                  key={event.id}
                >
                  <div>
                    <div className="list-title-line">
                      <b>{event.name}</b>

                      <Badge
                        tone={event.status.toLowerCase()}
                      >
                        {event.status}
                      </Badge>
                    </div>

                    <small>
                      {formatDate(event.startsAt)}
                    </small>

                    {event.status === 'DRAFT' && (
                      <small className="draft-help">
                        Configura inventario y precios
                        antes de publicar.
                      </small>
                    )}
                  </div>

                  <div className="inline-actions">
                    {event.status === 'DRAFT' && (
                      <button
                        onClick={() => {
                          const newName =
                            window.prompt(
                              'Nombre del evento',
                              event.name,
                            )

                          if (newName) {
                            edit.mutate({
                              id: event.id,
                              body: {
                                name: newName,
                                description:
                                  event.description ?? '',
                                startsAt:
                                  event.startsAt,
                                endsAt:
                                  event.endsAt,
                              },
                            })
                          }
                        }}
                      >
                        Editar
                      </button>
                    )}

                    {event.status === 'DRAFT' && (
                      <button
                        disabled={
                          publish.isPending ||
                          remove.isPending
                        }
                        onClick={() =>
                          publish.mutate(event.id)
                        }
                      >
                        Publicar
                      </button>
                    )}

                    <button
                      disabled={clone.isPending}
                      onClick={() =>
                        clone.mutate(event.id)
                      }
                    >
                      Clonar
                    </button>

                    {event.status ===
                      'PUBLISHED' && (
                      <button
                        disabled={cancel.isPending}
                        onClick={() => {
                          if (
                            window.confirm(
                              '¿Deshabilitar este evento? Dejará de aparecer en la cartelera pública.',
                            )
                          ) {
                            cancel.mutate(event.id)
                          }
                        }}
                      >
                        Deshabilitar
                      </button>
                    )}

                    {event.status === 'DRAFT' && (
                      <button
                        className="danger-button"
                        disabled={
                          remove.isPending ||
                          publish.isPending
                        }
                        onClick={() => {
                          if (
                            window.confirm(
                              '¿Eliminar definitivamente este borrador y su inventario disponible? Esta acción no se puede deshacer.',
                            )
                          ) {
                            remove.mutate(event.id)
                          }
                        }}
                      >
                        Eliminar
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  )
}