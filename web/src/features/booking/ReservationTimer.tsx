import { useEffect, useRef, useState } from 'react'

export function ReservationTimer({
  reservedUntil,
  onExpire,
}: {
  reservedUntil?: string | null
  onExpire?: () => void
}) {
  const remaining = () => Math.max(0, new Date(reservedUntil ?? 0).getTime() - Date.now())
  const [ms, setMs] = useState(remaining)
  const notified = useRef(false)

  useEffect(() => {
    notified.current = false
    setMs(remaining())
    if (!reservedUntil) return
    const tick = () => {
      const next = remaining()
      setMs(next)
      if (next === 0 && !notified.current) {
        notified.current = true
        onExpire?.()
      }
    }
    tick()
    const timer = window.setInterval(tick, 1000)
    return () => window.clearInterval(timer)
  }, [reservedUntil, onExpire])

  if (!reservedUntil) return null
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60

  return <div className={`reservation-timer ${ms === 0 ? 'expired' : ''}`}>
    <span>{ms === 0 ? 'Tu reservación expiró' : 'Tiempo para completar tu compra'}</span>
    <strong>{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</strong>
  </div>
}
