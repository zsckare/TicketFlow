export const formatMoney = (value: string, currency: string) =>
  new Intl.NumberFormat('es-MX', { style: 'currency', currency }).format(Number(value))
export const formatDate = (value: string) =>
  new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
