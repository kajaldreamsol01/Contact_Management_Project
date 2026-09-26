import { useState } from 'react'
import { Autocomplete, Box, Button, Collapse, Grid, MenuItem, Paper, TextField, Typography, } from '@mui/material'
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined'
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined'
import KeyboardArrowUpOutlinedIcon from '@mui/icons-material/KeyboardArrowUpOutlined'
import KeyboardArrowDownOutlinedIcon from '@mui/icons-material/KeyboardArrowDownOutlined'
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined'
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined'
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined'
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined'
import DateRangeField, { addDays, formatShortDate, isValidDateRange, } from '../DateRangeField'
import type { AppliedFilter, ContactDropdownData, ContactFilters, } from './apis'
const toApiDate = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const getToday = () => toApiDate(new Date())
const getWeekStart = () => {
  const date = new Date()
  date.setDate(date.getDate() - 6)
  return toApiDate(date)
}
export const getDefaultContactFilters = (): ContactFilters => ({
  search: '',
  name: '',
  contactType: '',
  department: '',
  city: '',
  status: 'Active',
  fromDate: getWeekStart(),
  toDate: getToday(),
  sortBy: '',
  sortDirection: '',
})
export const getAppliedFilterLabels = (filters: ContactFilters): AppliedFilter[] => {
  const result: AppliedFilter[] = []
  const add = (key: keyof ContactFilters, title: string) => {
    const value = filters[key]
    if (value)
      result.push({ key, label: `${title}: ${value}` })
  }
  if (filters.fromDate || filters.toDate) {
    result.push({
      key: 'fromDate',
      label: `Date: ${formatShortDate(filters.fromDate) || '-'} to ${formatShortDate(filters.toDate) || '-'}`,
    })
  }
  add('status', 'Status')
  add('search', 'Search')
  add('contactType', 'Contact Type')
  add('department', 'Department')
  add('city', 'City')
  if (filters.sortBy)
    result.push({ key: 'sortBy', label: `Sort By: ${filters.sortBy === 'contactCode' ? 'Contact Code' : filters.sortBy === 'updatedAt' ? 'Updated Date' : 'Created Date'}` })
  if (filters.sortDirection)
    result.push({ key: 'sortDirection', label: `Order: ${filters.sortDirection === 'asc' ? 'Ascending' : 'Descending'}` })
  return result
}
type TileKey = 'all' | 'active' | 'inactive'
type Props = {
  value: ContactFilters
  dropdowns: ContactDropdownData
  searching?: boolean
  total: number
  active: number
  inactive: number
  selectedTile: TileKey
  mode: 'date' | 'beginning'
  onModeChange: (mode: 'date' | 'beginning') => void
  onChange: <K extends keyof ContactFilters>(field: K, value: ContactFilters[K]) => void
  onWildSearch: (search: string) => void
  onSearch: (filters: ContactFilters) => void
  onReset: (filters: ContactFilters) => void
  onTileChange: (value: TileKey) => void
  nameSuggestions?: string[]
  onNameQueryChange?: (query: string) => void
}
const fieldSx = {
  '& .MuiOutlinedInput-root': {
    minHeight: 40, height: 40, backgroundColor: '#fff', borderRadius: '7px',
  },
  '& .MuiInputBase-input': { fontSize: 13, py: 0 },
  '& .MuiInputLabel-root': { fontSize: 13 },
  '& .MuiFormHelperText-root': { marginLeft: 0 },
}
function ContactActivityFilter({ value, dropdowns, searching = false, total, active, inactive, selectedTile, mode, onModeChange, onChange, onWildSearch, onSearch, onReset, onTileChange, nameSuggestions = [], onNameQueryChange, }: Props) {
  const [showMore, setShowMore] = useState(false)
  const today = getToday()
  const tiles = [
    {
      key: 'all' as const,
      label: 'All Contacts',
      value: total,
      icon: <GroupsOutlinedIcon fontSize='small' />,
      border: '#0f9187',
      background: '#138174',
      iconBackground: '#c8efeb',
      iconColor: '#0f9187',
    },
    {
      key: 'active' as const,
      label: 'Active',
      value: active,
      icon: <CheckCircleOutlineOutlinedIcon fontSize='small' />,
      border: '#22a35a',
      background: '#055220',
      iconBackground: '#d1f1dc',
      iconColor: '#95ebba',
    },
    {
      key: 'inactive' as const,
      label: 'Inactive',
      value: inactive,
      icon: <BlockOutlinedIcon fontSize='small' />,
      border: '#e44b52',
      background: '#ce131f',
      iconBackground: '#ffdfe1',
      iconColor: '#790b16',
    },
  ]
  const handleWildSearch = () => {
    const search = value.search.trim()
    if (!search)
      return
    setShowMore(false)
    onWildSearch(search)
  }
  const handleSearch = () => {
    setShowMore(false)
    onSearch({ ...value })
  }
  const handleReset = () => {
    setShowMore(false)
    onReset(getDefaultContactFilters())
  }
  const handleFromDate = (next: string) => {
    if (!next || next > today) return
    onChange('fromDate', next)
  }
  const handleToDate = (next: string) => {
    if (!value.fromDate || !next)
      return
    if (next > today || next < value.fromDate)
      return
    if (!isValidDateRange(value.fromDate, next))
      return
    onChange('toDate', next)
  }
  return (<Grid container spacing={1.25} sx={{ mb: 1, alignItems: 'flex-start' }}>
    <Grid size={{ xs: 12, md: 6 }}>
      <Paper elevation={0} sx={{
        p: 1.5, minHeight: 172, boxSizing: 'border-box',
        border: '1px solid #d6e4e1', borderRadius: 1.5, bgcolor: '#fff',
      }}>
        {/* Wild Search */}
        <Box sx={{ position: 'relative' }}>
          <Box onClick={handleWildSearch} sx={{
            position: 'absolute', right: 12, top: '50%',
            transform: 'translateY(-50%)', zIndex: 2,
            color: '#64748b', cursor: 'pointer', display: 'flex',
          }}>
            <SearchOutlinedIcon />
          </Box>

          <Autocomplete freeSolo disableClearable options={nameSuggestions} inputValue={value.search} onInputChange={(_, inputValue, reason) => {
            if (reason === 'input' || reason === 'clear') {
              onChange('search', inputValue)
              onNameQueryChange?.(inputValue)
            }
          }} onChange={(_, selected) => {
            if (typeof selected === 'string')
              onChange('search', selected)
          }} filterOptions={(options) => options} renderInput={(params) => (<TextField {...params} fullWidth size='small' label='Wild Search' sx={{
            ...fieldSx,
            '& .MuiInputBase-input': {
              pr: '44px !important',
              fontSize: 13,
            },
          }} />)} />
        </Box>

        {/* Date */}
        <Grid container spacing={1} sx={{ mt: 1.25 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DateRangeField label='From Date' value={value.fromDate} max={today} onChange={handleFromDate} sx={fieldSx} />
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <DateRangeField label='To Date' value={value.toDate} min={value.fromDate || undefined} max={value.fromDate && addDays(value.fromDate, 6) < today
              ? addDays(value.fromDate, 6)
              : today} onChange={handleToDate} sx={fieldSx} />
          </Grid>
        </Grid>

        <Collapse in={showMore} timeout={180} unmountOnExit>
          <Grid container spacing={1} sx={{ mt: 1, '& > .MuiGrid-root': { display: 'flex' } }}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='Contact Type' value={value.contactType} onChange={(e) => onChange('contactType', e.target.value)} sx={fieldSx}>
                <MenuItem value=''>All Contact Types</MenuItem>
                {dropdowns.contactTypes.map((item) => (<MenuItem key={item} value={item}>{item}</MenuItem>))}
              </TextField>
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='Department' value={value.department} onChange={(e) => onChange('department', e.target.value)} sx={fieldSx}>
                <MenuItem value=''>All Departments</MenuItem>
                {dropdowns.departments.map((item) => (<MenuItem key={item} value={item}>{item}</MenuItem>))}
              </TextField>
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='City' value={value.city} onChange={(e) => onChange('city', e.target.value)} sx={fieldSx}>
                <MenuItem value=''>All Cities</MenuItem>
                {dropdowns.cities.map((item) => (<MenuItem key={item} value={item}>{item}</MenuItem>))}
              </TextField>
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='Status' value={value.status} onChange={(e) => onChange('status', e.target.value as ContactFilters['status'])} sx={fieldSx}>
                <MenuItem value=''>All Status</MenuItem>
                {dropdowns.statuses.map((item) => (<MenuItem key={item} value={item}>{item}</MenuItem>))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='Sort By' value={value.sortBy} onChange={(e) => {
                const sortBy = e.target.value as ContactFilters['sortBy']
                onChange('sortBy', sortBy)
                if (!sortBy) onChange('sortDirection', '')
              }} sx={fieldSx}>
                <MenuItem value=''>Select Sort</MenuItem>
                <MenuItem value='contactCode'>Contact Code</MenuItem>
                <MenuItem value='createdAt'>Created Date</MenuItem>
                <MenuItem value='updatedAt'>Updated Date</MenuItem>
              </TextField>
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select fullWidth size='small' label='Order' value={value.sortDirection} disabled={!value.sortBy} onChange={(e) => onChange('sortDirection', e.target.value as ContactFilters['sortDirection'])} sx={fieldSx}>
                <MenuItem value=''>Select Order</MenuItem>
                <MenuItem value='asc'>Ascending</MenuItem>
                <MenuItem value='desc'>Descending</MenuItem>
              </TextField>
            </Grid>
          </Grid>
        </Collapse>

        {/* Draft filters do not change the table until Search is clicked. */}
        <Grid container spacing={1} sx={{ mt: 1.25 }}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <Button fullWidth variant='contained' disabled={searching} startIcon={<SearchOutlinedIcon />} onClick={handleSearch} sx={{
              height: 38, textTransform: 'none',
              fontWeight: 700, bgcolor: '#0f9187',
            }}>
              {searching ? 'Searching...' : 'Search'}
            </Button>
          </Grid>

          <Grid size={{ xs: 12, sm: 4 }}>
            <Button fullWidth variant='outlined' startIcon={<TuneOutlinedIcon />} endIcon={showMore
              ? <KeyboardArrowUpOutlinedIcon />
              : <KeyboardArrowDownOutlinedIcon />} onClick={() => setShowMore((current) => !current)} sx={{
                height: 38, textTransform: 'none', fontWeight: 700,
                borderColor: '#0f9187', color: '#0f9187',
              }}>
              {showMore ? 'Less Filter' : 'More Filter'}
            </Button>
          </Grid>

          <Grid size={{ xs: 12, sm: 4 }}>
            <Button fullWidth variant='outlined' startIcon={<RestartAltOutlinedIcon />} onClick={handleReset} sx={{
              height: 38, textTransform: 'none',
              fontWeight: 700, color: '#475569',
            }}>
              Reset
            </Button>
          </Grid>
        </Grid>
      </Paper>
    </Grid>

    {/* Count Tiles */}
    <Grid size={{ xs: 12, md: 6 }}>
      <Paper elevation={0} sx={{
        p: 1.25, minHeight: 172, boxSizing: 'border-box',
        border: '1px solid #d6e4e1', borderRadius: 1.5, bgcolor: '#fff',
      }}>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: .75, mb: .75 }}>
          <Button size='small' variant={mode === 'date' ? 'contained' : 'outlined'} onClick={() => void onModeChange('date')} sx={{ minHeight: 28, py: .25, textTransform: 'none', fontSize: 11, fontWeight: 700, bgcolor: mode === 'date' ? '#138174' : undefined, color: mode === 'date' ? '#fff' : '#0f9187', borderColor: '#0f9187', '&:hover': { bgcolor: mode === 'date' ? '#0f766e' : '#eef9f7', borderColor: '#0f9187' } }}>
            As Per Date Filter
          </Button>
          <Button size='small' variant={mode === 'beginning' ? 'contained' : 'outlined'} onClick={() => void onModeChange('beginning')} sx={{ minHeight: 28, py: .25, textTransform: 'none', fontSize: 11, fontWeight: 700, bgcolor: mode === 'beginning' ? '#138174' : undefined, color: mode === 'beginning' ? '#fff' : '#0f9187', borderColor: '#0f9187', '&:hover': { bgcolor: mode === 'beginning' ? '#0f766e' : '#eef9f7', borderColor: '#0f9187' } }}>
            Since Beginning
          </Button>
        </Box>
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(3,minmax(0,1fr))',
          },
          gap: 1.25,
        }}>
          {tiles.map((tile) => {
            const selected = selectedTile === tile.key
            return (<Paper key={tile.key} component='button' type='button' elevation={0} onClick={() => onTileChange(tile.key)} sx={{
              height: 108,
              p: 1,
              border: `${selected ? 2 : 1}px solid ${selected ? tile.border : '#d7e2e0'}`,
              borderRadius: 1.5,
              bgcolor: tile.background,
              cursor: 'pointer',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Box sx={{
                width: 32, height: 32, borderRadius: '50%',
                display: 'grid', placeItems: 'center',
                bgcolor: tile.iconBackground,
                color: tile.iconColor, mb: 0.7,
              }}>
                {tile.icon}
              </Box>

              <Typography sx={{ fontSize: 20, fontWeight: 800, color: '#fff' }}>
                {tile.value}
              </Typography>

              <Typography sx={{
                mt: 0.55, fontSize: 12,
                fontWeight: 700, color: '#e2e8f0',
              }}>
                {tile.label}
              </Typography>
            </Paper>)
          })}
        </Box>
      </Paper>
    </Grid>
  </Grid>)
}
export default ContactActivityFilter
