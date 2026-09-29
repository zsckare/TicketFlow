import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { AdminLayout } from './components/AdminLayout'
import { EventsPage } from './pages/EventsPage'
import { EventDetailPage } from './pages/EventDetailPage'
import { DashboardPage } from './pages/admin/DashboardPage'
import { VenuesPage } from './pages/admin/VenuesPage'
import { VenueDetailPage } from './pages/admin/VenueDetailPage'
import { SectionPage } from './pages/admin/SectionPage'
import { AdminEventsPage } from './pages/admin/AdminEventsPage'
import { InventoryPage } from './pages/admin/InventoryPage'
import { OrdersPage } from './pages/admin/OrdersPage'

export default function App() {
  return <Routes>
    <Route element={<Layout/>}>
      <Route index element={<Navigate replace to="/events"/>}/>
      <Route path="events" element={<EventsPage/>}/>
      <Route path="events/:eventId" element={<EventDetailPage/>}/>
      <Route path="admin" element={<AdminLayout/>}>
        <Route index element={<DashboardPage/>}/>
        <Route path="venues" element={<VenuesPage/>}/>
        <Route path="venues/:venueId" element={<VenueDetailPage/>}/>
        <Route path="sections/:sectionId" element={<SectionPage/>}/>
        <Route path="events" element={<AdminEventsPage/>}/>
        <Route path="inventory" element={<InventoryPage/>}/>
        <Route path="orders" element={<OrdersPage/>}/>
      </Route>
      <Route path="*" element={<Navigate replace to="/events"/>}/>
    </Route>
  </Routes>
}
