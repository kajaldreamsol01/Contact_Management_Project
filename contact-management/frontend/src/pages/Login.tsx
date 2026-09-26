import { useState, type ChangeEvent, type FormEvent, } from 'react'
import { Alert, Box, Button, Card, CardContent, } from '@mui/material'
import { useNavigate, } from 'react-router-dom'
import InputField from '../pages/app/components/InputField'
import api from '../api/axios'
import { fetchContacts, getDropdowns, setDropdownCache, setSession, type AppRole, } from './app/components/contact-master/apis'
import { useAppDispatch } from '../store/hooks'
const normalizeRoles = (value: unknown): AppRole[] => {
  const raw = Array.isArray(value)
    ? value
    : value
      ? [value]
      : []
  return raw
    .map((role) => String(role || '')
      .trim()
      .toUpperCase()
      .replace(/^ROLE_/, ''))
    .filter((role): role is AppRole => ['ADMIN', 'HOD', 'MANAGEMENT', 'USER'].includes(role))
}
function Login() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const [form, setForm] = useState({
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const change = (e: ChangeEvent<HTMLInputElement>) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    })
    if (error) {
      setError('')
    }
  }
  const handleLogin = async (e: FormEvent) => {
    e.preventDefault()
    const email = form.email.trim()
    const password = form.password
    if (!email || !password) {
      setError('Email and password are required')
      return
    }
    try {
      localStorage.removeItem('basicAuth')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('tokenType')
      localStorage.removeItem('roles')
      localStorage.removeItem('isLoggedIn')
      localStorage.removeItem('loggedInUser')
      const response = await api.post('/auth/login', { email, password })
      const result = response.data
      const accessToken = result?.data?.accessToken
      if (String(result?.status || '').toUpperCase() !== 'SUCCESS' ||
        !accessToken) {
        throw new Error(result?.error ||
          result?.message ||
          'Invalid email or password')
      }
      const roles = normalizeRoles(result?.data?.roles ??
        result?.data?.role)
      const loggedInUser = result?.data?.email || email
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('tokenType', result?.data?.tokenType || 'Bearer')
      localStorage.setItem('roles', JSON.stringify(roles))
      localStorage.setItem('isLoggedIn', 'true')
      localStorage.setItem('loggedInUser', loggedInUser)
      dispatch(setSession({
        email: loggedInUser,
        roles,
      }))
      const toDate = new Date()
      const fromDate = new Date()
      fromDate.setDate(fromDate.getDate() - 6)
      const dateText = (value: Date) => value
        .toISOString()
        .slice(0, 10)
      try {
        await Promise.all([
          dispatch(fetchContacts({
            status: false,
            fromDate: dateText(fromDate),
            toDate: dateText(toDate),
            page: 0,
            size: 10,
            sort: 'id',
            direction: 'desc',
          })).unwrap(),
          getDropdowns()
            .then((data) => dispatch(setDropdownCache(data))),
        ])
      }
      catch {
        // Login successful hai.
        // Preload fail ho to target page apna data baad me retry karega.
      }
      navigate(roles.includes('ADMIN')
        ? '/dashboard'
        : '/contacts', { replace: true })
    }
    catch {
      setError('Invalid email or password')
    }
  }
  return (<Box sx={{
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    bgcolor: '#f5f7fb',
    p: 2,
  }}>
    <Card sx={{
      width: '100%',
      maxWidth: 400,
      borderRadius: 3,
      boxShadow: '0 8px 30px rgba(0,0,0,.08)',
    }}>
      <CardContent sx={{
        p: 4,
      }}>
        <Box component='h1' sx={{
          textAlign: 'center',
          fontSize: 30,
          fontWeight: 700,
          m: 0,
        }}>
          Login
        </Box>

        <Box component='p' sx={{
          textAlign: 'center',
          color: 'text.secondary',
          mb: 3,
        }}>
          Contact Management System
        </Box>

        <Box component='form' onSubmit={handleLogin}>
          <InputField label='Email' name='email' value={form.email} onChange={change} required />

          <InputField label='Password' name='password' type='password' value={form.password} onChange={change} required />

          {error && (<Alert severity='error' sx={{
            mt: 1.5,
          }}>
            {error}
          </Alert>)}

          <Button type='submit' fullWidth variant='contained' sx={{
            mt: 2,
            py: 1.2,
            borderRadius: 2,
            textTransform: 'none',
            fontWeight: 600,
            bgcolor: '#0f766e',
            '&:hover': {
              bgcolor: '#115e59',
            },
          }}>
            Login
          </Button>
        </Box>

        <Box sx={{
          mt: 2,
          p: 1.5,
          borderRadius: 2,
          bgcolor: '#f0fdfa',
          color: '#475569',
          fontSize: 13,
          lineHeight: 1.7,
        }}>
          Temporary Login
          <br />

          Email:{' '}
          <strong>
            admin@dreamsol.com
          </strong>

          <br />

          Password:{' '}
          <strong>
            Admin@123
          </strong>
        </Box>
      </CardContent>
    </Card>
  </Box>)
}
export default Login
