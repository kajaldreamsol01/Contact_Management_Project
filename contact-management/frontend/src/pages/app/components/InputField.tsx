import TextField from '@mui/material/TextField'
import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import type { ChangeEvent, FocusEventHandler } from 'react'

type CommonProps = {
  label: string
  type?: string
  required?: boolean
  isRequired?: boolean
  error?: boolean
  helperText?: string
  errorMessage?: string
  disabled?: boolean
  multiline?: boolean
  rows?: number
  max?: string
}

type EventProps = CommonProps & {
  name: string
  value: string
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
  onBlur?: FocusEventHandler<HTMLInputElement>
  control?: never
}

type FormProps<T extends FieldValues> = CommonProps & {
  name: Path<T>
  control: Control<T>
  value?: never
  onChange?: never
  onBlur?: never
}

type Props<T extends FieldValues = FieldValues> = EventProps | FormProps<T>

function InputField<T extends FieldValues = FieldValues>(props: Props<T>) {
  const {
    label,
    type = 'text',
    required = false,
    isRequired = false,
    error = false,
    helperText = '',
    errorMessage = '',
    disabled = false,
    multiline = false,
    rows,
    max,
  } = props

  const slotProps = type === 'date'
    ? {
        inputLabel: { shrink: true },
        htmlInput: { max },
      }
    : max
      ? { htmlInput: { max } }
      : undefined

  if ('control' in props && props.control) {
    return (
      <Controller
        name={props.name as Path<T>}
        control={props.control}
        render={({ field }) => (
          <TextField
            {...field}
            value={field.value ?? ''}
            fullWidth
            size="small"
            label={label}
            type={type}
            multiline={multiline}
            rows={rows}
            required={required || isRequired}
            disabled={disabled}
            error={error}
            helperText={errorMessage || helperText || ' '}
            slotProps={slotProps}
            sx={{ '& .MuiFormLabel-asterisk': { color: 'error.main' } }}
            onChange={(event) => {
              const value = event.target.value

              if (type === 'date' && max && value && value > max)
                return

              field.onChange(event)
            }}
          />
        )}
      />
    )
  }

  return (
    <TextField
      fullWidth
      margin="normal"
      label={label}
      name={props.name}
      value={props.value}
      type={type}
      required={required || isRequired}
      disabled={disabled}
      multiline={multiline}
      rows={rows}
      error={error}
      helperText={helperText || errorMessage}
      slotProps={slotProps}
      sx={{ '& .MuiFormLabel-asterisk': { color: 'error.main' } }}
      onChange={(event: ChangeEvent<HTMLInputElement>) => {
        const value = event.target.value

        if (type === 'date' && max && value && value > max)
          return

        props.onChange(event)
      }}
      onBlur={props.onBlur}
    />
  )
}

export default InputField