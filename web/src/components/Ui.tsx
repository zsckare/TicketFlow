import type { ReactNode } from 'react'
export function Loading({ text = 'Cargando…' }: { text?: string }) { return <div className="state loading-state"><span className="spinner" />{text}</div> }
export function ErrorState({ error }: { error: Error }) { return <div className="alert error"><b>No pudimos completar la acción.</b><span>{error.message}</span></div> }
export function Empty({ children }: { children: ReactNode }) { return <div className="empty"><span className="empty-icon">◇</span><b>Sin información por ahora</b><p>{children}</p></div> }
export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: string }) { return <span className={`badge ${tone}`}>{children}</span> }
