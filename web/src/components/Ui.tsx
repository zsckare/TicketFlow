import type { ReactNode } from 'react'
export function Loading({ text = 'Cargando…' }: { text?: string }) {
  return <div className="state">{text}</div>
}
export function ErrorState({ error }: { error: Error }) {
  return <div className="alert error">{error.message}</div>
}
export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}
export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: string }) {
  return <span className={`badge ${tone}`}>{children}</span>
}
