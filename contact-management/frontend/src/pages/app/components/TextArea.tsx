import TextField from '@mui/material/TextField'
import type { ChangeEvent } from 'react'
type Props = {
  label: string
  name: string
  value: string
  rows?: number
  required?: boolean
  error?: boolean
  helperText?: string
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
}
function TextArea({ label, name, value, rows = 3, required = false, error = false, helperText = '', onChange, }: Props) {
  return (<TextField fullWidth multiline rows={rows} margin='normal' label={label} name={name} value={value} required={required} error={error} helperText={helperText} onChange={onChange} />)
}
export default TextArea
