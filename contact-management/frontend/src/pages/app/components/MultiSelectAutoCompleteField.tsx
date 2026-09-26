import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import { Autocomplete, Box, Checkbox, Chip, TextField, Typography, } from '@mui/material'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
const SELECT_ALL = '__SELECT_ALL__'
type Props<T extends FieldValues> = {
  name: Path<T>
  control: Control<T>
  label: string
  options: string[]
  error?: boolean
  errorMessage?: string
}
function MultiSelectAutoCompleteField<T extends FieldValues>({ name, control, label, options, error = false, errorMessage = '', }: Props<T>) {
  return (<Controller name={name} control={control} render={({ field }) => {
    const value: string[] = Array.isArray(field.value)
      ? field.value.filter((item: unknown): item is string => typeof item === 'string')
      : []
    const allSelected = options.length > 0 &&
      options.every((option) => value.includes(option))
    return (<Autocomplete<string, true, false, true> multiple freeSolo disableCloseOnSelect forcePopupIcon popupIcon={<KeyboardArrowDownIcon />} options={[SELECT_ALL, ...options]} value={value} onChange={(_, selected) => {
      const selectedValues: string[] = selected.filter((item): item is string => typeof item === 'string')
      if (selectedValues.includes(SELECT_ALL)) {
        const customValues = value.filter((item) => !options.includes(item))
        field.onChange(allSelected
          ? customValues
          : [...customValues, ...options])
        return
      }
      field.onChange(selectedValues.filter((item) => item !== SELECT_ALL))
    }} getOptionLabel={(option) => option === SELECT_ALL ? 'Select All' : option} renderOption={(props, option, { selected }) => (<li {...props} key={option}>
      <Checkbox checked={option === SELECT_ALL ? allSelected : selected} sx={{ mr: 1 }} />
      {option === SELECT_ALL ? 'Select All' : option}
    </li>)} renderValue={(selectedValues, getItemProps) => {
      if (!selectedValues.length)
        return null
      const firstValue = selectedValues[0]
      const remaining = selectedValues.length - 1
      const { key, ...itemProps } = getItemProps({ index: 0 })
      return (<Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <Chip key={key} {...itemProps} label={firstValue} size='small' />

        {remaining > 0 && (<Typography component='span' sx={{
          fontSize: 13,
          fontWeight: 600,
          color: 'text.secondary',
          whiteSpace: 'nowrap',
        }}>
          +{remaining}
        </Typography>)}
      </Box>)
    }} renderInput={(params) => (<TextField {...params} label={label} size='small' error={error} helperText={errorMessage || ' '} />)} />)
  }} />)
}
export default MultiSelectAutoCompleteField
