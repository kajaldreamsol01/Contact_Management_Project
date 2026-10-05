import type { ChangeEvent } from 'react'
import { Box, Button, Typography, } from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
type Props = {
  label: string
  accept?: string
  multiple?: boolean
  files?: File[]
  file?: File | null
  onChange: (value: File | File[]) => void
}
function FileField({ label, accept, multiple = false, files = [], file = null, onChange, }: Props) {
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = event.target.files
    if (!selectedFiles)
      return
    if (multiple) {
      onChange(Array.from(selectedFiles))
    }
    else {
      const selectedFile = selectedFiles[0]
      if (selectedFile) {
        onChange(selectedFile)
      }
    }
    /*
     * Same file dobara select
     * karne ke liye input reset.
     */
    event.target.value = ''
  }
  return (<Box>
    <Button component='label' variant='outlined' startIcon={<UploadFileIcon />} sx={{
      textTransform: 'none',
      fontWeight: 600,
    }}>
      {label}

      <input hidden type='file' accept={accept} multiple={multiple} onChange={handleChange} />
    </Button>

    {!multiple && file && (<Typography variant='caption' sx={{
      display: 'block',
      mt: 0.5,
      maxWidth: 220,
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    }}>
      {file.name}
    </Typography>)}

    {multiple &&
      files.length > 0 && (<Typography variant='caption' sx={{
        display: 'block',
        mt: 0.5,
      }}>
        {files.length} file(s)
        selected
      </Typography>)}
  </Box>)
}
export default FileField
