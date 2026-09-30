import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react'

import {
    authApi,
} from '../../api/authApi'

import {
    authStorage,
} from '../../api/httpClient'

import type {
    AuthUser,
    LoginRequest,
    RegisterRequest,
} from '../../types/auth'

type Ctx = {
    user: AuthUser | null
    loading: boolean
    login: (value: LoginRequest) => Promise<void>
    register: (value: RegisterRequest) => Promise<void>
    logout: () => Promise<void>
}

const AuthContext =
    createContext<Ctx | null>(null)

export function AuthProvider({
    children,
}: {
    children: ReactNode
}) {
    const [user, setUser] =
        useState<AuthUser | null>(null)

    const [loading, setLoading] =
        useState(true)

    /**
     * Restore the session when the application starts.
     *
     * If an access token exists, /users/me will use it and the HTTP
     * client will refresh it automatically when it has expired.
     *
     * If localStorage no longer contains an access token but the
     * HttpOnly refresh cookie is still valid, /auth/refresh restores
     * the complete browser session.
     */
    useEffect(() => {
        let cancelled = false

        const restoreSession = async () => {
            try {
                let restoredUser: AuthUser

                if (authStorage.get()) {
                    restoredUser =
                        await authApi.me()
                } else {
                    const response =
                        await authApi.refresh()

                    authStorage.set(
                        response.accessToken,
                    )

                    restoredUser =
                        response.user
                }

                if (!cancelled) {
                    setUser(restoredUser)
                }
            } catch {
                authStorage.clear()

                if (!cancelled) {
                    setUser(null)
                }
            } finally {
                if (!cancelled) {
                    setLoading(false)
                }
            }
        }

        void restoreSession()

        return () => {
            cancelled = true
        }
    }, [])

    const value = useMemo<Ctx>(
        () => ({
            user,
            loading,

            login: async value => {
                const response =
                    await authApi.login(value)

                authStorage.set(
                    response.accessToken,
                )

                setUser(
                    response.user,
                )
            },

            register: async value => {
                const response =
                    await authApi.register(value)

                authStorage.set(
                    response.accessToken,
                )

                setUser(
                    response.user,
                )
            },

            logout: async () => {
                await authApi.logout()
                setUser(null)
            },
        }),
        [
            user,
            loading,
        ],
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const value =
        useContext(AuthContext)

    if (!value) {
        throw new Error(
            'useAuth must be used inside AuthProvider',
        )
    }

    return value
}
