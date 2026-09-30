import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'
import { ordersApi } from '../api/ordersApi'
import { ticketsApi } from '../api/ticketsApi'
import { ErrorState, Loading } from '../components/Ui'
import { useAuth } from '../features/auth/AuthContext'
import { BookingCart } from '../features/booking/BookingCart'
import { GeneralAdmissionSelector } from '../features/booking/GeneralAdmissionSelector'
import { ReservedSeatSelector } from '../features/booking/ReservedSeatSelector'
import type { BookingSelection } from '../features/booking/types'
import { formatDate } from '../lib/format'
import type { OrderResponse } from '../types/orders'

export function EventDetailPage() {
  const { eventId = '' } = useParams()
  const qc = useQueryClient()
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [sectionId, setSectionId] = useState<string>()
  const [selected, setSelected] = useState<BookingSelection[]>([])
  const [order, setOrder] = useState<OrderResponse>()

  const event = useQuery({ queryKey: ['event', eventId], queryFn: () => eventsApi.getEvent(eventId) })
  const venue = useQuery({ queryKey: ['venue', event.data?.venueId], queryFn: () => eventsApi.getVenue(event.data!.venueId), enabled: !!event.data })
  const sections = useQuery({ queryKey: ['sections', event.data?.venueId], queryFn: () => eventsApi.getSections(event.data!.venueId), enabled: !!event.data })
  const activeSection = sectionId ?? sections.data?.[0]?.id
  const activeSectionData = sections.data?.find(section => section.id === activeSection)
  const seats = useQuery({ queryKey: ['seats', activeSection], queryFn: () => eventsApi.getSeats(activeSection!), enabled: !!activeSection && activeSectionData?.type === 'RESERVED_SEATING' })
  const inventory = useQuery({ queryKey: ['inventory', eventId], queryFn: () => ticketsApi.getEventInventory(eventId) })
  const activeOrder = useQuery({ queryKey: ['active-order', eventId, user?.id], queryFn: () => ordersApi.getActiveForEvent(eventId), enabled: !!user && !!eventId })
  const currentOrder = order ?? activeOrder.data ?? undefined

  const refreshInventory = () => qc.invalidateQueries({ queryKey: ['inventory', eventId] })
  const create = useMutation({ mutationFn: ordersApi.create, onSuccess: response => { setOrder(response); setSelected([]); void qc.invalidateQueries({ queryKey: ['active-order'] }); void refreshInventory() } })
  const confirm = useMutation({ mutationFn: ordersApi.confirm, onSuccess: response => { setOrder(response); setSelected([]); void qc.invalidateQueries({ queryKey: ['active-order'] }); void refreshInventory() } })
  const cancel = useMutation({ mutationFn: ordersApi.cancel, onSuccess: response => { setOrder(response); setSelected([]); void qc.invalidateQueries({ queryKey: ['active-order'] }); void refreshInventory() } })

  const inventoryBySeat = useMemo(() => new Map((inventory.data ?? []).filter(item => item.seatId).map(item => [item.seatId!, item])), [inventory.data])
  const generalAvailable = useMemo(() => (inventory.data ?? []).filter(item => item.sectionId === activeSection && !item.seatId && item.status === 'AVAILABLE'), [inventory.data, activeSection])
  const selectedIds = useMemo(() => new Set(selected.map(item => item.inventory.id)), [selected])
  const selectedGeneral = selected.filter(item => item.inventory.sectionId === activeSection && item.sectionType === 'GENERAL_ADMISSION')
  const busy = create.isPending || confirm.isPending || cancel.isPending
  const hasActiveOrder = currentOrder?.status === 'PENDING' || currentOrder?.status === 'RESERVED'
  const isReservedSection = activeSectionData?.type === 'RESERVED_SEATING'
  const isSelectionLoading = inventory.isPending || (isReservedSection && seats.isPending)

  const addGeneralTicket = () => {
    if (!activeSectionData) return
    const next = generalAvailable.find(item => !selectedIds.has(item.id))
    if (!next) return
    setSelected(previous => [...previous, { inventory: next, sectionName: activeSectionData.name, sectionType: activeSectionData.type }])
  }

  const removeGeneralTicket = () => {
    const last = selectedGeneral[selectedGeneral.length - 1]
    if (last) setSelected(previous => previous.filter(item => item.inventory.id !== last.inventory.id))
  }

  const removeSelection = (inventoryId: string) => setSelected(previous => previous.filter(item => item.inventory.id !== inventoryId))

  if (event.isPending) return <Loading />
  if (event.isError) return <ErrorState error={event.error} />

  return <main className="page event-detail-page">
    <Link className="back" to="/events">← Volver a eventos</Link>
    <section className="detail-hero">
      <div><span className="eyebrow">Evento en vivo</span><h1>{event.data.name}</h1><p>{event.data.description}</p></div>
      <div className="event-facts"><span><small>FECHA</small><b>{formatDate(event.data.startsAt)}</b></span>{venue.data && <span><small>LUGAR</small><b>{venue.data.name}</b><em>{venue.data.city}</em></span>}</div>
    </section>

    <div className="booking">
      <section className="panel booking-selector">
        <div className="booking-header"><div><span className="eyebrow">Boletos</span><h2>Elige dónde quieres estar</h2><p className="muted">Puedes combinar asientos numerados y admisión general en una misma compra.</p></div><span className="selection-pill">{selected.length} seleccionados</span></div>
        <div className="tabs section-tabs">{sections.data?.map(section => <button type="button" key={section.id} className={activeSection === section.id ? 'active' : ''} onClick={() => setSectionId(section.id)}><span>{section.name}</span><small>{section.type === 'GENERAL_ADMISSION' ? 'General' : 'Numerada'}</small></button>)}</div>

        {isSelectionLoading ? <Loading text="Preparando disponibilidad…" /> : activeSectionData?.type === 'GENERAL_ADMISSION' ?
          <GeneralAdmissionSelector sectionName={activeSectionData.name} available={generalAvailable} selectedCount={selectedGeneral.length} disabled={hasActiveOrder} onAdd={addGeneralTicket} onRemove={removeGeneralTicket} /> :
          <ReservedSeatSelector seats={seats.data ?? []} inventoryBySeat={inventoryBySeat} selectedIds={selectedIds} disabled={hasActiveOrder} onToggle={(item, seat) => {
            if (!activeSectionData) return
            setSelected(previous => previous.some(x => x.inventory.id === item.id)
              ? previous.filter(x => x.inventory.id !== item.id)
              : [...previous, { inventory: item, sectionName: activeSectionData.name, sectionType: activeSectionData.type, seatLabel: `${seat.row}${seat.number}` }])
          }} />}
      </section>

      <div>
        <BookingCart selections={selected} order={currentOrder} busy={busy} onRemove={removeSelection} onContinue={() => {
          if (!user) { navigate('/login', { state: { from: location.pathname } }); return }
          create.mutate(selected.map(item => item.inventory.id))
        }} onConfirm={() => currentOrder && confirm.mutate(currentOrder.id)} onCancel={() => currentOrder && cancel.mutate(currentOrder.id)} />
        {(create.error || confirm.error || cancel.error) && <ErrorState error={(create.error || confirm.error || cancel.error)!} />}
      </div>
    </div>
  </main>
}
