export class ApiError extends Error {
    readonly status: number

    constructor(message: string, status: number) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

const BASE =
    (import.meta.env.VITE_API_URL ?? '/api')
        .replace(/\/$/, '')

const TOKEN_KEY =
    'ticketflow.accessToken'

export const authStorage = {
    get: () =>
        localStorage.getItem(TOKEN_KEY),

    set: (value: string) =>
        localStorage.setItem(
            TOKEN_KEY,
            value,
        ),

    clear: () =>
        localStorage.removeItem(TOKEN_KEY),
}

let refreshPromise: Promise<boolean> | null = null

function buildUrl(path: string): string {
    if (path.startsWith('http')) {
        return path
    }

    return `${BASE}${path.startsWith('/') ? '' : '/'}${path}`
}

async function parseError(
    response: Response,
): Promise<ApiError> {
    let message =
        `Request failed with status ${response.status}`

    try {
        const body =
            await response.json() as {
                error?: string
                message?: string
            }

        message =
            body.error ??
            body.message ??
            message
    } catch {
        // Some error responses intentionally have no JSON body.
    }

    return new ApiError(
        message,
        response.status,
    )
}

/**
 * Uses the HttpOnly refresh-token cookie to obtain a new access token.
 * A single shared promise prevents several simultaneous 401 responses
 * from rotating the same refresh token concurrently.
 */
export async function refreshSession(): Promise<boolean> {
    if (refreshPromise) {
        return refreshPromise
    }

    refreshPromise = (async () => {
        const response = await fetch(
            buildUrl('/auth/refresh'),
            {
                method: 'POST',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )

        if (!response.ok) {
            authStorage.clear()
            return false
        }

        const body =
            await response.json() as {
                accessToken: string
            }

        authStorage.set(
            body.accessToken,
        )

        return true
    })().finally(() => {
        refreshPromise = null
    })

    return refreshPromise
}

export async function apiRequest<T>(
    path: string,
    options?: RequestInit,
    allowRefresh = true,
): Promise<T> {
    const token =
        authStorage.get()

    const response = await fetch(
        buildUrl(path),
        {
            ...options,
            credentials: 'include',
            headers: {
                Accept: 'application/json',
                ...(token
                    ? {
                        Authorization:
                            `Bearer ${token}`,
                    }
                    : {}),
                ...options?.headers,
            },
        },
    )

    /**
     * Access tokens are intentionally short lived. On the first 401,
     * rotate the refresh token and transparently retry the request once.
     */
    if (
        response.status === 401 &&
        allowRefresh &&
        path !== '/auth/login' &&
        path !== '/auth/register' &&
        path !== '/auth/refresh' &&
        path !== '/auth/logout'
    ) {
        const refreshed =
            await refreshSession()

        if (refreshed) {
            return apiRequest<T>(
                path,
                options,
                false,
            )
        }
    }

    if (!response.ok) {
        throw await parseError(response)
    }

    if (response.status === 204) {
        return undefined as T
    }

    return await response.json() as T
}

export async function apiDownload(path: string, allowRefresh = true): Promise<Blob> {
    const token = authStorage.get()
    const response = await fetch(buildUrl(path), {
        credentials: 'include',
        headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
    })
    if (response.status === 401 && allowRefresh && await refreshSession()) {
        return apiDownload(path, false)
    }
    if (!response.ok) throw await parseError(response)
    return response.blob()
}

export function json(
    body: unknown,
    method = 'POST',
): RequestInit {
    return {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
    }
}
