import { useRef } from 'react'
import { Box, InputAdornment, TextField } from '@mui/material'
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
export const MAX_RANGE_DAYS = 7
const parseApiDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}
export const addDays = (value: string, days: number) => {
  const date = parseApiDate(value)
  date.setDate(date.getDate() + days)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
export const formatShortDate = (value?: string) => {
  if (!value)
    return ''
  const [year, month, day] = value.split('-')
  return `${day}-${month}-${year.slice(-2)}`
}
export const isValidDateRange = (fromDate: string, toDate: string) => {
  if (!fromDate || !toDate || fromDate > toDate)
    return false
  return toDate <= addDays(fromDate, MAX_RANGE_DAYS - 1)
}
type Props = {
  label: string
  value: string
  min?: string
  max?: string
  onChange: (value: string) => void
  sx?: object
}
function DateRangeField({ label, value, min, max, onChange, sx }: Props) {
  const pickerRef = useRef<HTMLInputElement | null>(null)
  const openPicker = () => {
    const input = pickerRef.current
    if (!input)
      return
    if (typeof input.showPicker === 'function')
      input.showPicker()
    else
      input.click()
  }
  return (<Box sx={{ position: 'relative' }}>
    <TextField fullWidth size='small' label={label} value={formatShortDate(value)} onClick={openPicker} slotProps={{
      inputLabel: { shrink: true },
      htmlInput: {
        readOnly: true,
        'aria-label': `${label}, format DD-MM-YY`,
        style: { cursor: 'pointer' },
      },
      input: {
        endAdornment: (
          <InputAdornment position='end'>
            <CalendarMonthOutlinedIcon sx={{ fontSize: 20, color: '#1f2937', cursor: 'pointer' }} />
          </InputAdornment>
        ),
      },
    }} sx={{
      ...sx,
      '& .MuiOutlinedInput-root': { minHeight: 40, height: 40, borderRadius: '7px', bgcolor: '#fff' },
      '& .MuiInputBase-input': { fontSize: 13, cursor: 'pointer' },
      '& .MuiInputLabel-root': { fontSize: 13 },
    }} />
    <input ref={pickerRef} type='date' value={value} min={min} max={max} onChange={(event) => onChange(event.target.value)} tabIndex={-1} aria-hidden='true' style={{
      position: 'absolute',
      width: 1,
      height: 1,
      opacity: 0,
      pointerEvents: 'none',
      right: 0,
      bottom: 0,
    }} />
  </Box>)
}
export default DateRangeField
