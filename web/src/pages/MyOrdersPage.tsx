import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    useNavigate,
} from 'react-router-dom'

import {
    ordersApi,
} from '../api/ordersApi'

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

import {
    ReservationTimer,
} from '../features/booking/ReservationTimer'

export function MyOrdersPage() {
    const navigate =
        useNavigate()

    const qc =
        useQueryClient()

    const query =
        useQuery({
            queryKey: [
                'my-orders',
            ],
            queryFn:
                ordersApi.getAll,
        })

    const refresh =
        async () => {
            await Promise.all([
                qc.invalidateQueries({
                    queryKey: [
                        'my-orders',
                    ],
                }),

                qc.invalidateQueries({
                    queryKey: [
                        'inventory',
                    ],
                }),

                qc.invalidateQueries({
                    queryKey: [
                        'active-order',
                    ],
                }),

                qc.invalidateQueries({
                    queryKey: [
                        'my-tickets',
                    ],
                }),
            ])
        }

    const cancel =
        useMutation({
            mutationFn:
                ordersApi.cancel,

            onSuccess:
                refresh,
        })

    if (query.isPending) {
        return <Loading />
    }

    if (query.isError) {
        return (
            <ErrorState
                error={query.error}
            />
        )
    }

    return (
        <main className="page account-page">
            <div className="page-heading">
                <div>
                    <span className="eyebrow">
                        Tu cuenta
                    </span>

                    <h1>
                        Mis órdenes
                    </h1>

                    <p>
                        Consulta tus compras,
                        reservaciones y boletos
                        incluidos.
                    </p>
                </div>

                <span className="count-label">
                    {query.data.length} órdenes
                </span>
            </div>

            {cancel.error && (
                <ErrorState
                    error={cancel.error}
                />
            )}

            {!query.data.length ? (
                <Empty>
                    Aún no has realizado ninguna
                    compra.
                </Empty>
            ) : (
                <div className="order-cards">
                    {query.data.map(
                        order => {
                            const processing =
                                cancel.isPending &&
                                cancel.variables ===
                                order.id

                            const first =
                                order.items[0]

                            return (
                                <article
                                    className="panel order-card order-card-detailed"
                                    key={order.id}
                                >
                                    <div className="order-main">
                                        <div className="order-title">
                                            <div>
                                                <small>
                                                    ORDEN #
                                                    {order.id
                                                        .slice(0, 8)
                                                        .toUpperCase()}
                                                </small>

                                                <h3>
                                                    {first?.eventName ??
                                                        'Compra de boletos'}
                                                </h3>

                                                <p className="muted">
                                                    {first?.venueName}

                                                    {first?.eventStartsAt
                                                        ? ` · ${formatDate(first.eventStartsAt)}`
                                                        : ''}
                                                </p>
                                            </div>

                                            <Badge
                                                tone={
                                                    order.status.toLowerCase()
                                                }
                                            >
                                                {order.status}
                                            </Badge>
                                        </div>

                                        <div className="order-items-preview">
                                            {order.items.map(
                                                item => (
                                                    <div
                                                        key={item.id}
                                                    >
                                                        <span>
                                                            {item.sectionName ??
                                                                'Sección'}
                                                            {' · '}
                                                            {item.seatLabel ??
                                                                'Admisión general'}
                                                        </span>

                                                        <b>
                                                            {formatMoney(
                                                                item.unitPrice,
                                                                item.currency,
                                                            )}
                                                        </b>
                                                    </div>
                                                ),
                                            )}
                                        </div>

                                        <div className="order-meta">
                                            <span>
                                                {order.items.length}{' '}
                                                {order.items.length ===
                                                    1
                                                    ? 'boleto'
                                                    : 'boletos'}
                                            </span>

                                            <span>
                                                Creada{' '}
                                                {formatDate(
                                                    order.createdAt,
                                                )}
                                            </span>
                                        </div>
                                    </div>

                                    <div className="order-side">
                                        <div className="order-amount">
                                            <small>
                                                TOTAL
                                            </small>

                                            <strong>
                                                {formatMoney(
                                                    order.amount,
                                                    order.currency,
                                                )}
                                            </strong>
                                        </div>

                                        {order.status ===
                                            'RESERVED' && (
                                                <ReservationTimer
                                                    reservedUntil={
                                                        order.reservedUntil
                                                    }
                                                />
                                            )}
                                    </div>

                                    {order.status ===
                                        'RESERVED' && (
                                            <div className="order-actions">
                                                <button
                                                    className="button primary"
                                                    disabled={processing}
                                                    onClick={() =>
                                                        navigate(
                                                            '/checkout',
                                                        )
                                                    }
                                                >
                                                    Ir a pagar
                                                </button>

                                                <button
                                                    className="button secondary"
                                                    disabled={processing}
                                                    onClick={() =>
                                                        cancel.mutate(
                                                            order.id,
                                                        )
                                                    }
                                                >
                                                    {processing
                                                        ? 'Cancelando…'
                                                        : 'Cancelar reservación'}
                                                </button>
                                            </div>
                                        )}

                                    {order.status ===
                                        'CONFIRMED' && (
                                            <div className="order-note success-text">
                                                ✓ Compra completada ·
                                                tus accesos están en
                                                Mis boletos
                                            </div>
                                        )}

                                    {order.status ===
                                        'CANCELLED' && (
                                            <div className="order-note muted">
                                                Reservación cancelada
                                            </div>
                                        )}

                                    {order.status ===
                                        'FAILED' && (
                                            <div className="order-note">
                                                No fue posible completar
                                                esta orden.
                                            </div>
                                        )}
                                </article>
                            )
                        },
                    )}
                </div>
            )}
        </main>
    )
}