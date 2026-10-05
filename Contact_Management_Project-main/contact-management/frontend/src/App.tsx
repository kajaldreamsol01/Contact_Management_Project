import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import DashboardLayout from './layouts/DashboardLayout'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import ContactActivityBoard from './pages/app/components/contact-master/ContactActivityBoard'
import { readAccessProfile } from './pages/app/components/contact-master/apis'
function ProtectedLayout() {
  return localStorage.getItem('isLoggedIn') === 'true'
    ? <DashboardLayout />
    : <Navigate to='/login' replace />
}
function DashboardRoute() {
  return readAccessProfile().dashboardAccess ? <Dashboard /> : <Navigate to='/contacts' replace />
}
function App() {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'
  return (<BrowserRouter>
    <Routes>
      <Route path='/' element={<Navigate to={isLoggedIn ? '/contacts' : '/login'} replace />} />
      <Route path='/login' element={isLoggedIn ? <Navigate to='/contacts' replace /> : <Login />} />
      <Route path='/register' element={<Register />} />
      <Route element={<ProtectedLayout />}>
        <Route path='/dashboard' element={<DashboardRoute />} />
        <Route path='/contacts' element={<ContactActivityBoard />} />
      </Route>
      <Route path='*' element={<Navigate to={isLoggedIn ? '/contacts' : '/login'} replace />} />
    </Routes>
  </BrowserRouter>)
}
export default App
