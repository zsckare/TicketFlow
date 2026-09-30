import {
    Navigate,
    Route,
    Routes,
} from 'react-router-dom'

import {
    Layout,
} from './components/Layout'

import {
    AdminLayout,
} from './components/AdminLayout'

import {
    ProtectedRoute,
} from './components/ProtectedRoute'

import {
    EventsPage,
} from './pages/EventsPage'

import {
    EventDetailPage,
} from './pages/EventDetailPage'

import {
    LoginPage,
} from './pages/LoginPage'

import {
    RegisterPage,
} from './pages/RegisterPage'

import {
    MyOrdersPage,
} from './pages/MyOrdersPage'

import {
    NotificationsPage,
} from './pages/NotificationsPage'

import {
    MyTicketsPage,
} from './pages/MyTicketsPage'

import {
    CartPage,
} from './pages/CartPage'

import {
    CheckoutPage,
} from './pages/CheckoutPage'

import {
    CheckoutSuccessPage,
} from './pages/CheckoutSuccessPage'

import { CheckoutCancelPage } from './pages/CheckoutCancelPage'

import {
    DashboardPage,
} from './pages/admin/DashboardPage'

import {
    VenuesPage,
} from './pages/admin/VenuesPage'

import {
    VenueDetailPage,
} from './pages/admin/VenueDetailPage'

import {
    SectionPage,
} from './pages/admin/SectionPage'

import {
    AdminEventsPage,
} from './pages/admin/AdminEventsPage'

import {
    InventoryPage,
} from './pages/admin/InventoryPage'

import {
    OrdersPage,
} from './pages/admin/OrdersPage'

import {
    CheckInPage,
} from './pages/admin/CheckInPage'

import { UsersPage } from './pages/admin/UsersPage'

export default function App() {
    return (
        <Routes>
            <Route
                element={
                    <Layout />
                }
            >
                <Route
                    index
                    element={
                        <Navigate
                            replace
                            to="/events"
                        />
                    }
                />

                {/* Public catalog */}
                <Route
                    path="events"
                    element={
                        <EventsPage />
                    }
                />

                <Route
                    path="events/:eventId"
                    element={
                        <EventDetailPage />
                    }
                />

                {/* Authentication */}
                <Route
                    path="login"
                    element={
                        <LoginPage />
                    }
                />

                <Route
                    path="register"
                    element={
                        <RegisterPage />
                    }
                />

                {/* Customer cart */}
                <Route
                    path="cart"
                    element={
                        <ProtectedRoute>
                            <CartPage />
                        </ProtectedRoute>
                    }
                />

                {/*
         * Checkout is protected because it operates
         * on the authenticated user's active reservation.
         */}
                <Route
                    path="checkout"
                    element={
                        <ProtectedRoute>
                            <CheckoutPage />
                        </ProtectedRoute>
                    }
                />

                {/*
         * Stripe and the SIMULATED provider both finish
         * on this page.
         *
         * The page verifies the actual Order status rather
         * than trusting the browser redirect itself.
         */}
                <Route
                    path="checkout/success"
                    element={
                        <ProtectedRoute>
                            <CheckoutSuccessPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="checkout/cancel"
                    element={
                        <ProtectedRoute>
                            <CheckoutCancelPage />
                        </ProtectedRoute>
                    }
                />

                {/* Customer account */}
                <Route
                    path="orders"
                    element={
                        <ProtectedRoute>
                            <MyOrdersPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="notifications"
                    element={
                        <ProtectedRoute>
                            <NotificationsPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="tickets"
                    element={
                        <ProtectedRoute>
                            <MyTicketsPage />
                        </ProtectedRoute>
                    }
                />

                <Route path="staff/check-in" element={<ProtectedRoute role="STAFF"><main className="page"><CheckInPage /></main></ProtectedRoute>} />

                {/* Administration */}
                <Route
                    path="admin"
                    element={
                        <ProtectedRoute
                            role="ADMIN"
                        >
                            <AdminLayout />
                        </ProtectedRoute>
                    }
                >
                    <Route
                        index
                        element={
                            <DashboardPage />
                        }
                    />

                    <Route
                        path="venues"
                        element={
                            <VenuesPage />
                        }
                    />

                    <Route
                        path="venues/:venueId"
                        element={
                            <VenueDetailPage />
                        }
                    />

                    <Route
                        path="sections/:sectionId"
                        element={
                            <SectionPage />
                        }
                    />

                    <Route
                        path="events"
                        element={
                            <AdminEventsPage />
                        }
                    />

                    <Route
                        path="inventory"
                        element={
                            <InventoryPage />
                        }
                    />

                    <Route
                        path="orders"
                        element={
                            <OrdersPage />
                        }
                    />

                    <Route path="users" element={<UsersPage />} />

                    <Route
                        path="check-in"
                        element={
                            <CheckInPage />
                        }
                    />
                </Route>

                {/* Unknown route */}
                <Route
                    path="*"
                    element={
                        <Navigate
                            replace
                            to="/events"
                        />
                    }
                />
            </Route>
        </Routes>
    )
}