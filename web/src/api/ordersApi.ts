import {
    ApiError,
    apiRequest,
    json,
} from './httpClient'

import type {
    CheckoutResponse,
    OrderResponse,
} from '../types/orders'

async function getActive(): Promise<OrderResponse | null> {
    try {
        return await apiRequest<OrderResponse>(
            '/orders/active',
        )
    } catch (error) {
        if (
            error instanceof ApiError &&
            error.status === 404
        ) {
            return null
        }

        throw error
    }
}

async function getActiveForEvent(
    eventId: string,
): Promise<OrderResponse | null> {
    try {
        return await apiRequest<OrderResponse>(
            `/orders/active?eventId=${encodeURIComponent(eventId)}`,
        )
    } catch (error) {
        if (
            error instanceof ApiError &&
            error.status === 404
        ) {
            return null
        }

        throw error
    }
}

export const ordersApi = {
    getAll: () =>
        apiRequest<OrderResponse[]>(
            '/orders',
        ),

    get: (id: string) =>
        apiRequest<OrderResponse>(
            `/orders/${id}`,
        ),

    getActive,

    getActiveForEvent,

    create: (
        inventoryIds: string[],
    ) =>
        apiRequest<OrderResponse>(
            '/orders',
            json({
                inventoryIds,
            }),
        ),

    /**
     * Starts payment for an existing RESERVED order.
     *
     * The backend determines successUrl/cancelUrl for now.
     *
     * The order itself is NOT confirmed by this request.
     * Confirmation happens from Payments Service after the
     * payment succeeds.
     */
    /**
   * Starts payment for an existing RESERVED order.
   *
   * The frontend sends the URLs that the payment provider
   * should use after completing or cancelling checkout.
   *
   * The browser redirect is not considered payment confirmation.
   * The final confirmation is performed by Payments Service
   * through the payment callback/webhook.
   */
    checkout: (
        id: string,
    ) =>
        apiRequest<CheckoutResponse>(
            `/orders/${id}/checkout`,
            json({
                successUrl:
                    `${window.location.origin}/checkout/success?orderId=${encodeURIComponent(id)}`,
                cancelUrl:
                    `${window.location.origin}/cart`,
            }),
        ),

    cancel: (
        id: string,
    ) =>
        apiRequest<OrderResponse>(
            `/orders/${id}/cancel`,
            {
                method: 'POST',
            },
        ),
}