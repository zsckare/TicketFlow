import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/eventsApi'
import { ticketsApi } from '../../api/ticketsApi'
import { Badge, ErrorState, Loading } from '../../components/Ui'
import { formatMoney } from '../../lib/format'

export function InventoryPage() {
  const qc = useQueryClient()
  const events = useQuery({ queryKey: ['events'], queryFn: eventsApi.getEvents })
  const [eventId, setEventId] = useState('')
  const event = events.data?.find(x => x.id === eventId)
  const sections = useQuery({
    queryKey: ['sections', event?.venueId],
    queryFn: () => eventsApi.getSections(event!.venueId),
    enabled: !!event,
  })
  const inventory = useQuery({
    queryKey: ['inventory', eventId],
    queryFn: () => ticketsApi.getEventInventory(eventId),
    enabled: !!eventId,
  })
  const configs = useQuery({
    queryKey: ['inventory-configs', eventId],
    queryFn: () => ticketsApi.getSectionConfigs(eventId),
    enabled: !!eventId,
  })
  const refresh = async () => {
    await Promise.all([
      qc.invalidateQueries({ queryKey: ['inventory', eventId] }),
      qc.invalidateQueries({ queryKey: ['inventory-configs', eventId] }),
    ])
  }

  if (events.isPending) return <Loading />
  if (events.isError) return <ErrorState error={events.error} />

  return <section>
    <div className="section-title"><div><h2>Inventario</h2><p className="muted">Configura precio y disponibilidad por sección antes de publicar.</p></div></div>
    <div className="panel form">
      <label>Evento
        <select value={eventId} onChange={e => setEventId(e.target.value)}>
          <option value="">Selecciona…</option>
          {events.data.map(x => <option key={x.id} value={x.id}>{x.name} · {x.status}</option>)}
        </select>
      </label>
      {event && event.status !== 'DRAFT' && <div className="alert info">Este evento ya no está en DRAFT. La configuración comercial queda bloqueada.</div>}
    </div>

    {eventId && sections.data?.map(section =>
      <SectionInventoryEditor
        key={section.id}
        eventId={eventId}
        eventIsDraft={event?.status === 'DRAFT'}
        section={section}
        config={configs.data?.find(c => c.sectionId === section.id)}
        inventory={inventory.data?.filter(i => i.sectionId === section.id) ?? []}
        onChanged={refresh}
      />
    )}
  </section>
}

function SectionInventoryEditor({ eventId, eventIsDraft, section, config, inventory, onChanged }: any) {
  const [price, setPrice] = useState(config?.basePrice ?? '0.00')
  const [currency, setCurrency] = useState(config?.currency ?? 'MXN')
  const [quantity, setQuantity] = useState(String(config?.capacity ?? section.capacity))
  useEffect(() => {
    setPrice(config?.basePrice ?? '0.00')
    setCurrency(config?.currency ?? 'MXN')
    setQuantity(String(config?.capacity ?? section.capacity))
  }, [config?.basePrice, config?.currency, config?.capacity, section.capacity])

  const seats = useQuery({
    queryKey: ['seats', section.id],
    queryFn: () => eventsApi.getSeats(section.id),
    enabled: section.type === 'RESERVED_SEATING',
  })
  const configure = useMutation({
    mutationFn: () => ticketsApi.configureSection(eventId, {
      sectionId: section.id,
      basePrice: price,
      currency,
      ...(section.type === 'GENERAL_ADMISSION' ? { quantity: Number(quantity) } : {}),
    }),
    onSuccess: onChanged,
  })
  const inventoryBySeat = useMemo(() => new Map(inventory.filter((i:any) => i.seatId).map((i:any) => [i.seatId, i])), [inventory])

  return <div className="panel" style={{ marginTop: 18 }}>
    <div className="section-title"><div><h3>{section.name}</h3><p className="muted">{section.type} · capacidad física {section.capacity}</p></div>{config && <Badge tone="available">CONFIGURADA</Badge>}</div>
    <form className="form" onSubmit={(e:FormEvent) => { e.preventDefault(); configure.mutate() }}>
      <label>Precio base de la sección<input value={price} onChange={e => setPrice(e.target.value)} disabled={!eventIsDraft} required /></label>
      <label>Moneda<input value={currency} onChange={e => setCurrency(e.target.value.toUpperCase())} maxLength={3} disabled={!eventIsDraft} required /></label>
      {section.type === 'GENERAL_ADMISSION' && <label>Boletos disponibles<input type="number" min={1} max={section.capacity} value={quantity} onChange={e => setQuantity(e.target.value)} disabled={!eventIsDraft} required /></label>}
      {eventIsDraft && <button className="button primary" disabled={configure.isPending}>{config ? 'Actualizar sección' : 'Generar inventario de sección'}</button>}
      {configure.error && <ErrorState error={configure.error} />}
    </form>

    {config && section.type === 'GENERAL_ADMISSION' && <p className="hint">{inventory.filter((i:any) => i.status === 'AVAILABLE').length} disponibles de {inventory.length} unidades · {formatMoney(config.basePrice, config.currency)}</p>}
    {config && section.type === 'RESERVED_SEATING' && <div style={{ marginTop: 18 }}>
      <h3>Precios individuales</h3>
      <p className="hint">Vacío = hereda {formatMoney(config.basePrice, config.currency)} de la sección.</p>
      <div className="mini-seat-grid">
        {seats.data?.map(seat => {
          const item:any = inventoryBySeat.get(seat.id)
          return <SeatPriceEditor key={seat.id} eventId={eventId} seat={seat} item={item} currency={config.currency} disabled={!eventIsDraft} onChanged={onChanged} />
        })}
      </div>
    </div>}
  </div>
}

function SeatPriceEditor({ eventId, seat, item, currency, disabled, onChanged }: any) {
  const [override, setOverride] = useState(item?.priceOverride ?? '')
  useEffect(() => setOverride(item?.priceOverride ?? ''), [item?.priceOverride])
  const update = useMutation({
    mutationFn: () => ticketsApi.updateSeatPrice(eventId, seat.id, { priceOverride: override.trim() || null }),
    onSuccess: onChanged,
  })
  return <div>
    <b>{seat.row}{seat.number}</b>
    <small>{item ? formatMoney(item.price, currency) : 'Sin inventario'}</small>
    {item && <>
      <input aria-label={`Precio ${seat.row}${seat.number}`} placeholder="Override" value={override} onChange={e => setOverride(e.target.value)} disabled={disabled} />
      {!disabled && <button type="button" onClick={() => update.mutate()} disabled={update.isPending}>Guardar</button>}
    </>}
  </div>
}
