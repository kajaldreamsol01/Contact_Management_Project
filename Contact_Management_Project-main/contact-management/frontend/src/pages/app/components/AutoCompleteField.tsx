import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import { Autocomplete, TextField } from '@mui/material'
type Props<T extends FieldValues> = {
  name: Path<T>
  control: Control<T>
  label: string
  options: string[]
  isRequired?: boolean
  disabled?: boolean
  freeSolo?: boolean
  error?: boolean
  errorMessage?: string
}
function AutoCompleteField<T extends FieldValues>({ name, control, label, options, isRequired = false, disabled = false, freeSolo = false, error = false, errorMessage = '', }: Props<T>) {
  return (<Controller name={name} control={control} render={({ field }) => (<Autocomplete freeSolo={freeSolo} disabled={disabled} options={options} value={field.value || ''} inputValue={field.value || ''} onChange={(_, value) => field.onChange(value || '')} onBlur={field.onBlur} onInputChange={(_, value, reason) => {
    if (freeSolo && (reason === 'input' || reason === 'clear'))
      field.onChange(value)
  }} renderInput={(params) => (<TextField {...params} fullWidth size='small' label={label} required={isRequired} error={error} helperText={errorMessage || ' '} sx={{
    '& .MuiFormLabel-asterisk': {
      color: '#d32f2f',
      fontWeight: 700,
    },
  }} />)} />)} />)
}
export default AutoCompleteField
