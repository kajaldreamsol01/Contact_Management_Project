import { useEffect, useState } from 'react'
import { Controller, useForm, type Path, type Resolver } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Alert, Box, Button, Grid, IconButton, MenuItem, Paper, TextField, Typography, } from '@mui/material'
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined'
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined'
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined'
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined'
import contactSchema from '../../../../validation/contactSchema'
import { uploadFiles, type ContactDetail, type ContactDropdownData, type ContactRequestDto, type ContactUploadRequest, } from './apis'
import type { ContactFormData } from './AddContactForm'
import InputField from '../InputField'
import AutoCompleteField from '../AutoCompleteField'
import MultiSelectAutoCompleteField from '../MultiSelectAutoCompleteField'
type Props = {
  data: ContactDetail
  dropdowns: ContactDropdownData
  onSave: (payload: ContactRequestDto) => Promise<void>
  onClose: () => void
}
const EMPTY: ContactFormData = {
  contactCode: '',
  contactType: '',
  name: '',
  communicationName: '',
  department: '',
  designation: '',
  companyName: '',
  mobile: '',
  alternateMobile: '',
  officeNumber: '',
  email: '',
  alternateEmail: '',
  employeeId: '',
  gender: '',
  maritalStatus: '',
  dateOfBirth: '',
  anniversaryDate: '',
  bloodGroup: '',
  country: '',
  state: '',
  city: '',
  address: '',
  pinCode: '',
  skills: [],
  languages: [],
  emergencyContactName: '',
  emergencyContactNumber: '',
  remarks: '',
  status: false,
  photoUuid: '',
  documentUuids: [],
  photo: null,
  documents: [],
}
const sectionSx = {
  p: 2,
  mb: 1.5,
  border: '1px solid #dce8e6',
  borderRadius: 2,
  boxShadow: 'none',
}
const titleSx = {
  mb: 1.5,
  pb: 0.75,
  fontSize: 14,
  fontWeight: 700,
  color: '#007f75',
  borderBottom: '1px solid #e5eeee',
}
function UpdateContactForm({ data, dropdowns, onSave, onClose }: Props) {
  const [saving, setSaving] = useState(false)
  const [apiError, setApiError] = useState('')
  const { control, handleSubmit, reset, setValue, watch, formState: { errors }, } = useForm<ContactFormData>({
    resolver: yupResolver(contactSchema) as unknown as Resolver<ContactFormData>,
    defaultValues: {
      ...EMPTY,
      ...data,
      dateOfBirth: data.dateOfBirth ?? '',
      anniversaryDate: data.anniversaryDate ?? '',
      photoUuid: data.photoUuid ?? '',
      documentUuids: data.documentUuids ?? [],
      photo: null,
      documents: [],
    },
    mode: 'onBlur',
  })
  useEffect(() => {
    reset({
      ...EMPTY,
      ...data,
      dateOfBirth: data.dateOfBirth ?? '',
      anniversaryDate: data.anniversaryDate ?? '',
      photoUuid: data.photoUuid ?? '',
      documentUuids: data.documentUuids ?? [],
      photo: null,
      documents: [],
    })
  }, [data, reset])
  const photoUuid = watch('photoUuid')
  const documentUuids = watch('documentUuids') || []
  const selectedPhoto = watch('photo')
  const selectedDocuments = watch('documents') || []
  const addOrEditDetails = async (form: ContactFormData) => {
    setSaving(true)
    setApiError('')
    try {
      const requests: ContactUploadRequest[] = []
      if (form.photo) {
        requests.push({ file: form.photo, type: 'PHOTO' })
      }
      (form.documents || []).forEach((file) => requests.push({ file, type: 'DOCUMENT' }))
      const uploaded = requests.length
        ? await uploadFiles(requests)
        : []
      let uploadIndex = 0
      let photoUuid = form.photoUuid || ''
      const documentUuids = [...new Set(form.documentUuids || [])]
      if (form.photo) {
        photoUuid = uploaded[uploadIndex++]?.uuid || photoUuid
      }
      while (uploadIndex < uploaded.length) {
        const uuid = uploaded[uploadIndex++]?.uuid
        if (uuid)
          documentUuids.push(uuid)
      }
      const { photo: _photo, documents: _documents, ...dto } = form
      await onSave({
        ...dto,
        photoUuid,
        documentUuids,
      })
    }
    catch (error) {
      setApiError(error instanceof Error
        ? error.message
        : 'Unable to save contact')
    }
    finally {
      setSaving(false)
    }
  }
  const fieldError = (name: keyof ContactFormData) => errors[name]?.message as string | undefined
  const text = (name: keyof ContactFormData, label: string, options?: {
    required?: boolean
    type?: string
    multiline?: boolean
    rows?: number
  }) => (<InputField name={name as Path<ContactFormData>} control={control} label={label} type={options?.type} multiline={options?.multiline} rows={options?.rows} isRequired={options?.required} error={Boolean(fieldError(name))} errorMessage={fieldError(name)} />)
  const select = (name: keyof ContactFormData, label: string, options: string[], required = false) => (<AutoCompleteField name={name as Path<ContactFormData>} control={control} label={label} options={options} isRequired={required} error={Boolean(fieldError(name))} errorMessage={fieldError(name)} />)
  return (<form onSubmit={handleSubmit(addOrEditDetails)} noValidate>
    <Box>
      {apiError && <Alert severity='error' sx={{ mb: 1.5 }}>{apiError}</Alert>}

      <Paper elevation={0} sx={sectionSx}>
        <Typography sx={titleSx}>Basic Information</Typography>
        <Grid container spacing={1.5}>
          <Grid size={{ xs: 12, sm: 6 }}>{text('contactCode', 'Contact Code')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{select('contactType', 'Contact Type', dropdowns.contactTypes, true)}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('name', 'Name', { required: true })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('communicationName', 'Communication Name')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('department', 'Department')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('designation', 'Designation')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('companyName', 'Company Name')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Controller name='status' control={control} render={({ field }) => (<TextField select fullWidth size='small' label='Status' value={field.value ? 'Inactive' : 'Active'} onChange={(event) => field.onChange(event.target.value === 'Inactive')} helperText=' '>
              <MenuItem value='Active'>Active</MenuItem>
              <MenuItem value='Inactive'>Inactive</MenuItem>
            </TextField>)} />
          </Grid>
        </Grid>
      </Paper>

      <Paper elevation={0} sx={sectionSx}>
        <Typography sx={titleSx}>Contact Information</Typography>
        <Grid container spacing={1.5}>
          <Grid size={{ xs: 12, sm: 6 }}>{text('mobile', 'Mobile', { required: true })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('alternateMobile', 'Alternate Mobile')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('officeNumber', 'Office Number')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('email', 'Email', { required: true })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('alternateEmail', 'Alternate Email')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('employeeId', 'Employee ID')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{select('gender', 'Gender', dropdowns.genders)}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{select('maritalStatus', 'Marital Status', dropdowns.maritalStatuses)}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('dateOfBirth', 'Date Of Birth', { type: 'date' })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('anniversaryDate', 'Anniversary Date', { type: 'date' })}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{select('bloodGroup', 'Blood Group', dropdowns.bloodGroups)}</Grid>
        </Grid>
      </Paper>

      <Paper elevation={0} sx={sectionSx}>
        <Typography sx={titleSx}>Address & Other Details</Typography>
        <Grid container spacing={1.5}>
          <Grid size={{ xs: 12, sm: 4 }}>{text('country', 'Country')}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{text('state', 'State')}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{text('city', 'City')}</Grid>
          <Grid size={{ xs: 12, sm: 8 }}>{text('address', 'Address')}</Grid>
          <Grid size={{ xs: 12, sm: 4 }}>{text('pinCode', 'Pin Code')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <MultiSelectAutoCompleteField name='skills' control={control} label='Skills' options={dropdowns.skills} error={Boolean(fieldError('skills'))} errorMessage={fieldError('skills')} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <MultiSelectAutoCompleteField name='languages' control={control} label='Languages' options={dropdowns.languages} error={Boolean(fieldError('languages'))} errorMessage={fieldError('languages')} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('emergencyContactName', 'Emergency Contact Name')}</Grid>
          <Grid size={{ xs: 12, sm: 6 }}>{text('emergencyContactNumber', 'Emergency Contact Number')}</Grid>
          <Grid size={{ xs: 12 }}>{text('remarks', 'Remarks', { multiline: true, rows: 3 })}</Grid>
        </Grid>
      </Paper>

      <Paper elevation={0} sx={sectionSx}>
        <Typography sx={titleSx}>Attachments</Typography>
        <Grid container spacing={1.5}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Button variant='outlined' component='label' fullWidth>
              {photoUuid || selectedPhoto ? 'Replace Photo' : 'Select Photo'}
              <input hidden type='file' accept='.jpg,.jpeg,.png' onChange={(event) => {
                const file = event.target.files?.[0] || null
                setValue('photo', file, { shouldDirty: true, shouldValidate: false })
                event.target.value = ''
              }} />
            </Button>

            {photoUuid && !selectedPhoto && (
              <Box sx={{ mt: .75, px: 1, py: .6, display: 'flex', alignItems: 'center', gap: 1, border: '1px solid #d6e4e1', borderRadius: 1, bgcolor: '#f8fbfa' }}>
                <Typography variant='caption' sx={{ flex: 1, color: '#0f766e', fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {data.photoFile?.uuid === photoUuid ? data.photoFile.fileName : 'Existing photo'}
                </Typography>
                <IconButton size='small' color='error' title='Remove photo' onClick={() => setValue('photoUuid', '', { shouldDirty: true })}>
                  <DeleteOutlineOutlinedIcon fontSize='small' />
                </IconButton>
              </Box>
            )}

            {selectedPhoto && (
              <Box sx={{ mt: .75, px: 1, py: .6, display: 'flex', alignItems: 'center', gap: 1, border: '1px solid #d6e4e1', borderRadius: 1, bgcolor: '#f8fbfa' }}>
                <Typography variant='caption' sx={{ flex: 1, color: '#0f766e', fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  New: {selectedPhoto.name}
                </Typography>
                <IconButton size='small' color='error' title='Remove selected photo' onClick={() => setValue('photo', null, { shouldDirty: true })}>
                  <DeleteOutlineOutlinedIcon fontSize='small' />
                </IconButton>
              </Box>
            )}
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Button variant='outlined' component='label' fullWidth>
              Add Documents
              <input hidden type='file' multiple accept='.pdf,.doc,.docx,.xls,.xlsx' onChange={(event) => {
                const files = Array.from(event.target.files || [])
                setValue('documents', [...selectedDocuments, ...files], { shouldDirty: true, shouldValidate: false })
                event.target.value = ''
              }} />
            </Button>

            {documentUuids.map((uuid) => {
              const file = data.documentFiles?.find((item) => item.uuid === uuid)
              return (
                <Box key={uuid} sx={{ mt: .75, px: 1, py: .6, display: 'flex', alignItems: 'center', gap: 1, border: '1px solid #d6e4e1', borderRadius: 1, bgcolor: '#f8fbfa' }}>
                  <Typography variant='caption' sx={{ flex: 1, color: '#0f766e', fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {file?.fileName || 'Existing document'}
                  </Typography>
                  <IconButton size='small' color='error' title='Remove document' onClick={() => setValue('documentUuids', documentUuids.filter((item) => item !== uuid), { shouldDirty: true })}>
                    <DeleteOutlineOutlinedIcon fontSize='small' />
                  </IconButton>
                </Box>
              )
            })}

            {selectedDocuments.map((file, index) => (
              <Box key={`${file.name}-${file.size}-${index}`} sx={{ mt: .75, px: 1, py: .6, display: 'flex', alignItems: 'center', gap: 1, border: '1px solid #d6e4e1', borderRadius: 1, bgcolor: '#f8fbfa' }}>
                <Typography variant='caption' sx={{ flex: 1, color: '#0f766e', fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  New: {file.name}
                </Typography>
                <IconButton size='small' color='error' title='Remove selected document' onClick={() => setValue('documents', selectedDocuments.filter((_, fileIndex) => fileIndex !== index), { shouldDirty: true })}>
                  <DeleteOutlineOutlinedIcon fontSize='small' />
                </IconButton>
              </Box>
            ))}
          </Grid>
        </Grid>
      </Paper>

      <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1.5, mt: 2 }}>
        <Button type='submit' variant='contained' startIcon={<SaveOutlinedIcon />} disabled={saving}>
          {saving ? 'Saving...' : 'Update'}
        </Button>
        <Button type='button' variant='outlined' startIcon={<RestartAltOutlinedIcon />} disabled={saving} onClick={() => reset({
          ...EMPTY,
          ...data,
          dateOfBirth: data.dateOfBirth ?? '',
          anniversaryDate: data.anniversaryDate ?? '',
          photoUuid: data.photoUuid ?? '',
          documentUuids: data.documentUuids ?? [],
          photo: null,
          documents: [],
        })}>
          Reset
        </Button>
        <Button type='button' variant='outlined' color='error' startIcon={<CloseOutlinedIcon />} disabled={saving} onClick={onClose}>
          Cancel
        </Button>
      </Box>
    </Box>
  </form>)
}
export default UpdateContactForm
