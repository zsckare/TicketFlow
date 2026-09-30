
import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import { ordersApi } from '../api/ordersApi'
import {
    Badge,
    Empty,
    ErrorState,
    Loading,
} from '../components/Ui'
import {
    formatDate,
    formatMoney,
} from '../lib/format'

export function MyOrdersPage() {
    const queryClient = useQueryClient()

    /*
     * ID de la orden sobre la que se está ejecutando
     * actualmente una acción.
     *
     * Lo usamos para deshabilitar únicamente los botones
     * de esa orden mientras se procesa la petición.
     */
    const confirmMutation = useMutation({
        mutationFn: (orderId: string) =>
            ordersApi.confirm(orderId),

        onSuccess: async () => {
            /*
             * Volvemos a consultar las órdenes para obtener
             * el nuevo estado desde el backend.
             */
            await queryClient.invalidateQueries({
                queryKey: ['my-orders'],
            })

            /*
             * Una confirmación puede cambiar inventario de
             * RESERVED -> SOLD.
             *
             * Invalidamos cualquier consulta de inventory,
             * independientemente del eventId.
             */
            await queryClient.invalidateQueries({
                queryKey: ['inventory'],
            })
        },
    })

    const cancelMutation = useMutation({
        mutationFn: (orderId: string) =>
            ordersApi.cancel(orderId),

        onSuccess: async () => {
            /*
             * Al cancelar esperamos que Orders libere las
             * reservaciones correspondientes en Tickets.
             */
            await queryClient.invalidateQueries({
                queryKey: ['my-orders'],
            })

            /*
             * Esto hará que la próxima vez que entremos al
             * evento se consulte nuevamente el inventario y
             * los asientos liberados aparezcan AVAILABLE.
             */
            await queryClient.invalidateQueries({
                queryKey: ['inventory'],
            })
        },
    })

    const ordersQuery = useQuery({
        queryKey: ['my-orders'],
        queryFn: ordersApi.getAll,
    })

    if (ordersQuery.isPending) {
        return <Loading />
    }

    if (ordersQuery.isError) {
        return (
            <ErrorState
                error={ordersQuery.error}
            />
        )
    }

    const actionError =
        confirmMutation.error ??
        cancelMutation.error

    return (
        <main className="page">
            <div className="section-title">
                <h1>Mis órdenes</h1>

                <span>
                    {ordersQuery.data.length}
                </span>
            </div>

            {actionError && (
                <ErrorState
                    error={actionError}
                />
            )}

            {!ordersQuery.data.length ? (
                <Empty>
                    Aún no tienes órdenes.
                </Empty>
            ) : (
                <div className="order-cards">
                    {ordersQuery.data.map(order => {
                        const isReserved =
                            order.status === 'RESERVED'

                        const isConfirming =
                            confirmMutation.isPending &&
                            confirmMutation.variables ===
                            order.id

                        const isCancelling =
                            cancelMutation.isPending &&
                            cancelMutation.variables ===
                            order.id

                        const isProcessing =
                            isConfirming ||
                            isCancelling

                        return (
                            <article
                                className="panel order-card"
                                key={order.id}
                            >
                                <div>
                                    <small>
                                        ORDEN
                                    </small>

                                    <code>
                                        {order.id}
                                    </code>
                                </div>

                                <b>
                                    {formatMoney(
                                        order.amount,
                                        order.currency,
                                    )}
                                </b>

                                <Badge
                                    tone={order.status.toLowerCase()}
                                >
                                    {order.status}
                                </Badge>

                                <span className="muted">
                                    {formatDate(
                                        order.createdAt,
                                    )}
                                </span>

                                {isReserved && (
                                    <div className="stack">
                                        <button
                                            type="button"
                                            className="button primary full"
                                            disabled={
                                                isProcessing
                                            }
                                            onClick={() =>
                                                confirmMutation.mutate(
                                                    order.id,
                                                )
                                            }
                                        >
                                            {isConfirming
                                                ? 'Confirmando...'
                                                : 'Confirmar compra'}
                                        </button>

                                        <button
                                            type="button"
                                            className="button secondary full"
                                            disabled={
                                                isProcessing
                                            }
                                            onClick={() =>
                                                cancelMutation.mutate(
                                                    order.id,
                                                )
                                            }
                                        >
                                            {isCancelling
                                                ? 'Cancelando...'
                                                : 'Cancelar reservación'}
                                        </button>
                                    </div>
                                )}

                                {order.status ===
                                    'CONFIRMED' && (
                                        <div className="alert success">
                                            Compra confirmada.
                                        </div>
                                    )}

                                {order.status ===
                                    'CANCELLED' && (
                                        <div className="alert info">
                                            Reservación cancelada.
                                        </div>
                                    )}

                                {order.status ===
                                    'FAILED' && (
                                        <div className="alert info">
                                            La orden no pudo
                                            completarse.
                                        </div>
                                    )}
                            </article>
                        )
                    })}
                </div>
            )}
        </main>
    )
}
