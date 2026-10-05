import { Checkbox, FormControlLabel } from '@mui/material'
type Props = {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}
function CheckboxField({ label, checked, onChange, }: Props) {
  return (<FormControlLabel label={label} control={<Checkbox checked={checked} onChange={(e) => onChange(e.target.checked)} />} />)
}
export default CheckboxField
