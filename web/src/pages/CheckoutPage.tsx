import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    Link,
    useNavigate,
} from 'react-router-dom'

import {
    ordersApi,
} from '../api/ordersApi'

import {
    ErrorState,
    Loading,
} from '../components/Ui'

import {
    ReservationTimer,
} from '../features/booking/ReservationTimer'

import {
    formatMoney,
} from '../lib/format'

export function CheckoutPage() {
    const navigate =
        useNavigate()

    const qc =
        useQueryClient()

    const active =
        useQuery({
            queryKey: [
                'active-order',
            ],
            queryFn:
                ordersApi.getActive,
        })

    const checkout =
        useMutation({
            mutationFn:
                async () => {
                    if (!active.data) {
                        throw new Error(
                            'No active order',
                        )
                    }

                    return ordersApi.checkout(
                        active.data.id,
                    )
                },

            onSuccess:
                response => {
                    /*
                     * Stripe:
                     *
                     * Redirect the customer to the external
                     * Checkout Session.
                     */
                    if (response.checkoutUrl) {
                        window.location.assign(
                            response.checkoutUrl,
                        )

                        return
                    }

                    /*
                     * SIMULATED:
                     *
                     * Payments Service has already called Orders
                     * synchronously before returning the response,
                     * so the order should now be CONFIRMED.
                     */
                    void qc.invalidateQueries({
                        queryKey: [
                            'active-order',
                        ],
                    })

                    void qc.invalidateQueries({
                        queryKey: [
                            'my-orders',
                        ],
                    })

                    void qc.invalidateQueries({
                        queryKey: [
                            'my-tickets',
                        ],
                    })

                    void qc.invalidateQueries({
                        queryKey: [
                            'inventory',
                        ],
                    })

                    navigate(
                        `/checkout/success?orderId=${response.order.id}`,
                    )
                },
        })

    if (active.isPending) {
        return (
            <Loading
                text="Preparando checkout…"
            />
        )
    }

    if (active.isError) {
        return (
            <ErrorState
                error={active.error}
            />
        )
    }

    const order =
        active.data

    if (!order) {
        return (
            <main className="page">
                <section className="panel empty-feature">
                    <h1>
                        No hay una reservación activa
                    </h1>

                    <p className="muted">
                        Selecciona tus boletos antes
                        de iniciar el checkout.
                    </p>

                    <Link
                        className="button primary"
                        to="/events"
                    >
                        Explorar eventos
                    </Link>
                </section>
            </main>
        )
    }

    return (
        <main className="page checkout-page">
            <div className="page-heading">
                <div>
                    <span className="eyebrow">
                        Checkout seguro
                    </span>

                    <h1>
                        Revisa y paga
                    </h1>

                    <p>
                        Confirma los detalles antes
                        de continuar al proveedor
                        de pago.
                    </p>
                </div>
            </div>

            <div className="checkout-grid">
                <section className="panel checkout-review">
                    <h2>
                        {order.items[0]?.eventName ??
                            'Tu evento'}
                    </h2>

                    <p className="muted">
                        {order.items[0]?.venueName}
                    </p>

                    <div className="checkout-lines">
                        {order.items.map(
                            item => (
                                <div key={item.id}>
                                    <span>
                                        <b>
                                            {item.seatLabel ??
                                                (
                                                    item.sectionType ===
                                                        'GENERAL_ADMISSION'
                                                        ? 'Admisión general'
                                                        : 'Boleto'
                                                )}
                                        </b>

                                        <small>
                                            {item.sectionName}
                                        </small>
                                    </span>

                                    <strong>
                                        {formatMoney(
                                            item.unitPrice,
                                            item.currency,
                                        )}
                                    </strong>
                                </div>
                            ),
                        )}
                    </div>

                    <ReservationTimer
                        reservedUntil={
                            order.reservedUntil
                        }
                    />
                </section>

                <aside className="panel payment-summary">
                    <span className="eyebrow">
                        Resumen
                    </span>

                    <div className="summary-row">
                        <span>
                            Boletos
                        </span>

                        <b>
                            {order.items.length}
                        </b>
                    </div>

                    <div className="summary-row total">
                        <span>
                            Total
                        </span>

                        <b>
                            {formatMoney(
                                order.amount,
                                order.currency,
                            )}
                        </b>
                    </div>

                    <button
                        className="button primary full button-lg"
                        disabled={
                            checkout.isPending
                        }
                        onClick={() =>
                            checkout.mutate()
                        }
                    >
                        {checkout.isPending
                            ? 'Procesando pago…'
                            : 'Pagar ahora'}
                    </button>

                    <Link
                        className="button secondary full"
                        to="/cart"
                    >
                        Volver al carrito
                    </Link>

                    <p className="secure-note">
                        <span>✓</span>{' '}
                        El pedido sólo se confirma
                        después de validar el pago.
                    </p>

                    {checkout.error && (
                        <ErrorState
                            error={checkout.error}
                        />
                    )}
                </aside>
            </div>
        </main>
    )
}