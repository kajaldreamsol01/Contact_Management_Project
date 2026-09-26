import { FormControl, FormHelperText, InputLabel, MenuItem, Select, type SelectChangeEvent, } from '@mui/material'
import type { FocusEventHandler } from 'react'
type Props = {
  label: string
  name: string
  value: string
  options: string[]
  required?: boolean
  error?: boolean
  helperText?: string
  onChange: (event: SelectChangeEvent) => void
  onBlur?: FocusEventHandler<HTMLInputElement>
}
function SelectField({ label, name, value, options, required = false, error = false, helperText = '', onChange, onBlur, }: Props) {
  return (<FormControl fullWidth margin='normal' required={required} error={error}>
    <InputLabel>{label}</InputLabel>
    <Select name={name} value={value} label={label} onChange={onChange} onBlur={onBlur}>
      {options.map((option) => (<MenuItem key={option} value={option}>
        {option}
      </MenuItem>))}
    </Select>
    {helperText && <FormHelperText>{helperText}</FormHelperText>}
  </FormControl>)
}
export default SelectField
