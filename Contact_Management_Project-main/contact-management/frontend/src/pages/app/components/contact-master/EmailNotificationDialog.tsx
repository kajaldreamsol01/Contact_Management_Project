import { useState, type FormEvent } from 'react'
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Stack, TextField, Typography, } from '@mui/material'
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined'
import CloseIcon from '@mui/icons-material/Close'
import SendOutlinedIcon from '@mui/icons-material/SendOutlined'
import { sendContactEmail } from './apis'
type Props = {
  open: boolean
  onClose: () => void
  onSuccess: (message: string) => void
}
function EmailNotificationDialog({ open, onClose, onSuccess }: Props) {
  const [attachmentName, setAttachmentName] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const close = () => {
    if (sending)
      return
    setAttachmentName('')
    setError('')
    onClose()
  }
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = event.currentTarget
    setSending(true)
    setError('')
    try {
      const result = await sendContactEmail(new FormData(form))
      form.reset()
      setAttachmentName('')
      onSuccess(result.message || 'Email sent successfully')
      onClose()
    }
    catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to send email')
    }
    finally {
      setSending(false)
    }
  }
  return (<Dialog open={open} onClose={() => { }} fullWidth maxWidth='sm'>
    <Box component='form' onSubmit={submit}>
      <DialogTitle sx={{
        bgcolor: '#0f9187', color: '#fff', fontWeight: 700,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1,
      }}>
        Email Notification
        <IconButton type='button' aria-label='Close email notification' onClick={close} disabled={sending} size='small' sx={{
          color: '#d32f2f', bgcolor: '#fff', border: '1px solid #d32f2f',
          '&:hover': { bgcolor: '#ffebee' },
          '&.Mui-disabled': {
            color: 'rgba(211,47,47,0.45)',
            bgcolor: 'rgba(255,255,255,0.8)',
          },
        }}>
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          {error && <Alert severity='error'>{error}</Alert>}
          <TextField name='to' label='To' type='email' fullWidth required size='small' />
          <TextField name='subject' label='Subject' fullWidth required size='small' />
          <TextField name='message' label='Message' fullWidth required multiline minRows={5} />

          <Button component='label' variant='outlined' startIcon={<AttachFileOutlinedIcon />}>
            {attachmentName || 'Add Attachment (Optional)'}
            <input hidden name='attachment' type='file' onChange={(e) => setAttachmentName(e.target.files?.[0]?.name || '')} />
          </Button>

          {attachmentName && (<Typography variant='body2' color='text.secondary'>
            Attached: {attachmentName}
          </Typography>)}
        </Stack>
      </DialogContent>

      <DialogActions sx={{ p: 2 }}>
        <Button type='submit' disabled={sending} variant='contained' startIcon={<SendOutlinedIcon />}>
          {sending ? 'Sending...' : 'Send Email'}
        </Button>
      </DialogActions>
    </Box>
  </Dialog>)
}
export default EmailNotificationDialog
