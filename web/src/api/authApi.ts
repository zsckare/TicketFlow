import {
    apiRequest,
    authStorage,
} from './httpClient'

import type {
    AuthResponse,
    AuthUser,
    LoginRequest,
    RegisterRequest,
} from '../types/auth'

export const authApi = {
    login: (
        body: LoginRequest,
    ) =>
        apiRequest<AuthResponse>(
            '/auth/login',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body),
            },
            false,
        ),

    register: (
        body: RegisterRequest,
    ) =>
        apiRequest<AuthResponse>(
            '/auth/register',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body),
            },
            false,
        ),

    refresh: () =>
        apiRequest<AuthResponse>(
            '/auth/refresh',
            {
                method: 'POST',
            },
            false,
        ),

    me: () =>
        apiRequest<AuthUser>(
            '/users/me',
        ),

    logout: async () => {
        try {
            await apiRequest<void>(
                '/auth/logout',
                {
                    method: 'POST',
                },
                false,
            )
        } finally {
            authStorage.clear()
        }
    },
}
