import { useEffect, useState } from 'react'

export function ReservationTimer({ reservedUntil }: { reservedUntil?: string | null }) {
  const remaining = () => Math.max(0, new Date(reservedUntil ?? 0).getTime() - Date.now())
  const [ms, setMs] = useState(remaining)

  useEffect(() => {
    setMs(remaining())
    if (!reservedUntil) return
    const timer = window.setInterval(() => setMs(remaining()), 1000)
    return () => window.clearInterval(timer)
  }, [reservedUntil])

  if (!reservedUntil) return null
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60

  return <div className={`reservation-timer ${ms === 0 ? 'expired' : ''}`}>
    <span>{ms === 0 ? 'Reserva vencida' : 'Tiempo para completar tu compra'}</span>
    <strong>{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</strong>
  </div>
}
