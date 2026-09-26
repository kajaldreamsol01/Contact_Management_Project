import { useState } from 'react'
import { Box, Button, Card, CardContent } from '@mui/material'
import { Link } from 'react-router-dom'
import InputField from '../pages/app/components/InputField'
function Register() {
  const [f, setF] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const change = (e: any) => setF({
    ...f,
    [e.target.name]: e.target.value,
  })
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
      maxWidth: 430,
      borderRadius: 3,
      boxShadow: '0 8px 30px rgba(0,0,0,.08)',
    }}>
      <CardContent sx={{ p: 4 }}>
        <Box component='h1' sx={{
          textAlign: 'center',
          fontSize: 30,
          fontWeight: 700,
          m: 0,
        }}>
          Register
        </Box>

        <Box component='p' sx={{
          textAlign: 'center',
          color: 'text.secondary',
          mb: 3,
        }}>
          Create your account
        </Box>

        <InputField label='Name' name='name' value={f.name} onChange={change} required />

        <InputField label='Email' name='email' value={f.email} onChange={change} required />

        <InputField label='Password' name='password' type='password' value={f.password} onChange={change} required />

        <InputField label='Confirm Password' name='confirmPassword' type='password' value={f.confirmPassword} onChange={change} required />

        <Button fullWidth variant='contained' sx={{
          mt: 2,
          py: 1.2,
          borderRadius: 2,
          textTransform: 'none',
          fontWeight: 600,
        }}>
          Register
        </Button>

        <Box sx={{
          textAlign: 'center',
          mt: 2,
          color: 'text.secondary',
        }}>
          Already have an account?{' '}
          <Link to='/'>Login</Link>
        </Box>
      </CardContent>
    </Card>
  </Box>)
}
export default Register
