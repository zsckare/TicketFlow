export const formatMoney = (value: string, currency: string) =>
  new Intl.NumberFormat('es-MX', { style: 'currency', currency }).format(Number(value))
export const formatDate = (value: string) =>
  new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

export const formatDateInTimeZone = (value: string, timeZone?: string | null) =>
  new Intl.DateTimeFormat('es-MX', {
    dateStyle: 'medium',
    timeStyle: 'short',
    ...(timeZone ? { timeZone } : {}),
  }).format(new Date(value))
