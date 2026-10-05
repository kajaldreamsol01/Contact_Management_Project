import { Checkbox, FormControl, InputLabel, ListItemText, MenuItem, Select, } from '@mui/material'
type Props = {
  label: string
  value: string[]
  options: string[]
  onChange: (value: string[]) => void
}
function MultiSelectField({ label, value, options, onChange }: Props) {
  const allSelected = options.length > 0 && value.length === options.length
  const handleChange = (selected: string[]) => {
    if (selected.includes('__ALL__')) {
      onChange(allSelected ? [] : options)
      return
    }
    onChange(selected)
  }
  return (<FormControl fullWidth margin='normal'>
    <InputLabel>{label}</InputLabel>

    <Select multiple value={value} label={label} renderValue={(selected) => selected.length === options.length
      ? 'All Selected'
      : selected.join(', ')} onChange={(event) => {
        const selected = typeof event.target.value === 'string'
          ? event.target.value.split(',')
          : event.target.value
        handleChange(selected)
      }}>
      <MenuItem value='__ALL__'>
        <Checkbox checked={allSelected} />
        <ListItemText primary='Select All' />
      </MenuItem>

      {options.map((option) => (<MenuItem key={option} value={option}>
        <Checkbox checked={value.includes(option)} />
        <ListItemText primary={option} />
      </MenuItem>))}
    </Select>
  </FormControl>)
}
export default MultiSelectField
