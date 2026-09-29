import axios from '../../../../api/axios'
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

const API = '/contact'
const MASTER_API = '/master'
const USER_API = '/users'

export type ContactStatus = 'Active' | 'Inactive'

export type ContactRequestDto = {
  id?: number
  contactCode: string
  contactType: string
  name: string
  communicationName: string
  department: string
  designation: string
  companyName: string
  mobile: string
  alternateMobile: string
  officeNumber: string
  email: string
  alternateEmail: string
  employeeId: string
  gender: string
  maritalStatus: string
  dateOfBirth: string
  anniversaryDate: string
  bloodGroup: string
  country: string
  state: string
  city: string
  address: string
  pinCode: string
  skills: string[]
  languages: string[]
  emergencyContactName: string
  emergencyContactNumber: string
  remarks: string
  status: boolean
  photoUuid: string
  documentUuids: string[]
}

export type ContactListItem = {
  id: number
  contactCode: string
  contactType: string
  name: string
  communicationName: string
  department: string
  designation: string
  companyName: string
  mobile: string
  alternateMobile: string
  officeNumber: string
  email: string
  alternateEmail: string
  employeeId: string
  gender: string
  maritalStatus: string
  dateOfBirth: string | null
  anniversaryDate: string | null
  bloodGroup: string
  country: string
  state: string
  city: string
  address: string
  pinCode: string
  skills: string[]
  languages: string[]
  emergencyContactName: string
  emergencyContactNumber: string
  remarks: string
  photoUuid?: string
  photoFile?: UploadedContactFile
  documentUuids: string[]
  documentFiles: UploadedContactFile[]
  status: boolean
  createdBy?: number | null
  createdByName?: string | null
  createdAt?: string
  updatedBy?: number | null
  updatedByName?: string | null
  updatedAt?: string
}

export type ContactDetail = ContactListItem

export type ContactHistoryChange = {
  oldValue?: unknown
  newValue?: unknown
}

export type ContactHistoryItem = {
  id: number
  contactId: number
  action: 'CREATED' | 'UPDATED' | 'INACTIVATED' | 'REACTIVATED' | string
  actionBy?: string | null
  changedAt: string
  fieldName?: string | null
  oldValue?: string | null
  newValue?: string | null
  contactCode?: string | null
  source?: string | null
  data?: Record<string, unknown>
  changes?: Record<string, ContactHistoryChange>
}

export type ContactFilters = {
  search: string
  name: string
  contactType: string
  department: string
  city: string
  status: ContactStatus | ''
  fromDate: string
  toDate: string
  sortBy: 'contactCode' | 'createdAt' | 'updatedAt' | ''
  sortDirection: 'asc' | 'desc' | ''
}

export type AppliedFilter = {
  key: keyof ContactFilters
  label: string
}

export type ContactDropdownData = {
  names: string[]
  contactTypes: string[]
  departments: string[]
  cities: string[]
  genders: string[]
  maritalStatuses: string[]
  bloodGroups: string[]
  skills: string[]
  languages: string[]
  statuses: ContactStatus[]
}

export type DropdownData = ContactDropdownData

export type ContactSearchParams = {
  search?: string
  name?: string
  contactType?: string
  department?: string
  city?: string
  status?: boolean
  fromDate?: string
  toDate?: string
}

export type ContactPageRequest = ContactSearchParams & {
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

export type ContactPage = {
  content: ContactListItem[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type ExcelPreviewRow = {
  action?: 'New' | 'Update'
  rowNumber: number
  contactType: string
  name: string
  mobile: string
  email: string
  department: string
  designation: string
  companyName: string
  city: string
  previousStatus?: 'Active' | 'Inactive' | null
  status?: 'Active' | 'Inactive'
  updateType?: string | null
  changedFields?: string | null
  error?: string
}

export type SaveResultItem = {
  index?: number
  rowNumber?: number
  name: string
  mobile: string
  email: string
  contactCode?: string
  action?: 'New Saved' | 'Updated'
  status?: 'Active' | 'Inactive'
  previousStatus?: 'Active' | 'Inactive' | null
  changedFields?: string | null
  message?: string
}

export type BulkSaveResult = {
  totalCount: number
  successCount: number
  newSavedCount: number
  updatedCount: number
  duplicateCount: number
  invalidCount: number
  saved: SaveResultItem[]
  newSaved: SaveResultItem[]
  updated: SaveResultItem[]
  duplicates: SaveResultItem[]
  invalid: SaveResultItem[]
}

export type ExcelImportResult = {
  totalRows: number
  totalCount?: number
  successCount: number
  newSavedCount: number
  updatedCount: number
  duplicateCount: number
  invalidCount: number
  saved: SaveResultItem[]
  newSaved: SaveResultItem[]
  updated: SaveResultItem[]
  duplicates: SaveResultItem[]
  invalid: SaveResultItem[]
}

export type AppNotification = {
  id: number
  title: string
  message: string
  type: 'SUCCESS' | 'ERROR' | 'APPROVAL' | string
  attachmentName?: string | null
  totalCount?: number | null
  savedCount?: number | null
  updatedCount?: number | null
  duplicateCount?: number | null
  invalidCount?: number | null
  audienceRole?: string | null
  audienceEmail?: string | null
  actionType?: string | null
  actionRequestId?: number | null
  actionStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | string | null
  isRead?: boolean
  read?: boolean
  createdAt: string
}

export type NotificationData = {
  items: AppNotification[]
  unreadCount: number
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ExcelValidationResult = {
  valid: boolean
  canSave: boolean
  records: ExcelPreviewRow[]
  newRecords: ExcelPreviewRow[]
  updateRecords: ExcelPreviewRow[]
  duplicates: ExcelPreviewRow[]
  invalid: ExcelPreviewRow[]
  newCount: number
  updateCount: number
  duplicateCount: number
  invalidCount: number
}

type ExcelBucket = 'new' | 'update' | 'duplicate' | 'invalid'

const excelRowKey = (row: ExcelPreviewRow, index: number) => row.rowNumber
  ? `row-${row.rowNumber}`
  : `${String(row.mobile || '').trim().toLowerCase()}|${String(row.email || '').trim().toLowerCase()}|${index}`

const excelFrontendValidationError = (row: ExcelPreviewRow & Record<string, any>) => {
  const contactType = String(row.contactType ?? '').trim()
  const name = String(row.name ?? '').trim()
  const mobile = String(row.mobile ?? '').trim()
  const email = String(row.email ?? '').trim()

  if (!contactType)
    return 'Contact Type is required'

  if (!name)
    return 'Name is required'

  if (!/^[A-Za-z ]+$/.test(name))
    return 'Name should contain alphabets only'

  if (!/^[6-9]\d{9}$/.test(mobile))
    return 'Enter valid 10 digit mobile number'

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
    return 'Enter valid Email ID'

  const alternateMobile = String(row.alternateMobile ?? '').trim()

  if (alternateMobile && !/^[6-9]\d{9}$/.test(alternateMobile))
    return 'Enter valid 10 digit alternate mobile number'

  if (alternateMobile && alternateMobile === mobile)
    return 'Alternate mobile must be different from mobile number'

  const alternateEmail = String(row.alternateEmail ?? '').trim()

  if (alternateEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(alternateEmail))
    return 'Enter valid Alternate Email'

  if (alternateEmail && alternateEmail.toLowerCase() === email.toLowerCase())
    return 'Alternate Email must be different from Email ID'

  const pinCode = String(row.pinCode ?? '').trim()

  if (pinCode && !/^\d{6}$/.test(pinCode))
    return 'Pin Code must be 6 digits'

  const emergency = String(row.emergencyContactNumber ?? '').trim()

  if (emergency && !/^[6-9]\d{9}$/.test(emergency))
    return 'Enter valid emergency contact number'

  return ''
}

const excelBucket = (
  row: ExcelPreviewRow & Record<string, any>,
  source?: ExcelBucket,
): ExcelBucket => {
  const result = String(row.error ?? row.message ?? '').trim()
  const lower = result.toLowerCase()
  const updateType = String(row.updateType ?? '').trim().toLowerCase()

  const duplicate =
    lower.includes('duplicate') ||
    lower.includes('already exist') ||
    lower.includes('already present') ||
    lower.includes('no change') ||
    updateType === 'no change'

  if (duplicate)
    return 'duplicate'

  const frontendError = excelFrontendValidationError(row)

  if (frontendError) {
    if (!result)
      row.error = frontendError
    return 'invalid'
  }

  if (result)
    return 'invalid'

  if (source === 'duplicate' || source === 'invalid')
    return source

  if (source === 'update' || source === 'new')
    return source

  if (
    String(row.action ?? '').toLowerCase() === 'update' ||
    row.previousStatus != null ||
    (updateType && updateType !== 'no change')
  )
    return 'update'

  return 'new'
}

const normalizeExcelValidation = (data: any): ExcelValidationResult => {
  const buckets: Record<ExcelBucket, ExcelPreviewRow[]> = {
    new: [],
    update: [],
    duplicate: [],
    invalid: [],
  }

  const seen = new Set<string>()

  const addRows = (rows: unknown, source?: ExcelBucket) => {
    if (!Array.isArray(rows))
      return

    rows.forEach((raw, index) => {
      const row = { ...(raw || {}) } as ExcelPreviewRow & Record<string, any>
      const key = excelRowKey(row, index)

      if (seen.has(key))
        return

      seen.add(key)

      const bucket = excelBucket(row, source)

      if (bucket === 'new')
        row.action = 'New'

      if (bucket === 'update')
        row.action = 'Update'

      buckets[bucket].push(row)
    })
  }

  addRows(data?.newRecords, 'new')
  addRows(data?.updateRecords, 'update')
  addRows(data?.duplicates, 'duplicate')
  addRows(data?.invalid, 'invalid')
  addRows(data?.records)

  const records = [
    ...buckets.new,
    ...buckets.update,
    ...buckets.duplicate,
    ...buckets.invalid,
  ]

  return {
    valid: buckets.invalid.length === 0,
    canSave: buckets.new.length + buckets.update.length > 0,
    records,
    newRecords: buckets.new,
    updateRecords: buckets.update,
    duplicates: buckets.duplicate,
    invalid: buckets.invalid,
    newCount: buckets.new.length,
    updateCount: buckets.update.length,
    duplicateCount: buckets.duplicate.length,
    invalidCount: buckets.invalid.length,
  }
}

const ensureSuccess = <T,>(data: any, message: string): T => {
  if (String(data?.status || '').toUpperCase() !== 'SUCCESS')
    throw new Error(data?.error || data?.message || message)

  return data as T
}

const notifyChanged = () => {
  window.dispatchEvent(new Event('contact-analytics-updated'))
  window.dispatchEvent(new Event('notification-updated'))
}

const normalizeSaveResult = (data: any): BulkSaveResult => ({
  totalCount: Number(data?.totalCount ?? data?.totalRows ?? 0),
  successCount: Number(data?.successCount || 0),
  newSavedCount: Number(data?.newSavedCount || 0),
  updatedCount: Number(data?.updatedCount || 0),
  duplicateCount: Number(data?.duplicateCount || 0),
  invalidCount: Number(data?.invalidCount || 0),
  saved: Array.isArray(data?.saved) ? data.saved : [],
  newSaved: Array.isArray(data?.newSaved) ? data.newSaved : [],
  updated: Array.isArray(data?.updated) ? data.updated : [],
  duplicates: Array.isArray(data?.duplicates) ? data.duplicates : [],
  invalid: Array.isArray(data?.invalid) ? data.invalid : [],
})

const getContactPage = async (
  endpoint: 'fetch' | 'filter',
  params: ContactPageRequest = {},
): Promise<ContactPage> => {
  const {
    page = 0,
    size = 10,
    sort = 'id',
    direction = 'desc',
    ...filters
  } = params

  const response = await axios.get(`${API}/${endpoint}`, {
    params: {
      ...filters,
      page,
      size,
      sort,
      direction,
    },
  })

  const result = ensureSuccess<any>(
    response.data,
    'Unable to fetch contacts',
  )

  const data = result.data || {}

  return {
    content: Array.isArray(data.content) ? data.content : [],
    number: Number(data.number || 0),
    size: Number(data.size || size),
    totalElements: Number(data.totalElements || 0),
    totalPages: Number(data.totalPages || 0),
    first: Boolean(data.first),
    last: Boolean(data.last),
  }
}

export const fetchContactPage = (
  params: ContactPageRequest = {},
) => getContactPage('fetch', params)

export const filterContactPage = (
  params: ContactPageRequest = {},
) => getContactPage('filter', params)

type ContactState = ContactPage & {
  loading: boolean
  loaded: boolean
  error: string | null
}

const initialContactState: ContactState = {
  content: [],
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
  loading: false,
  loaded: false,
  error: null,
}

export const fetchContacts = createAsyncThunk(
  'contacts/fetch',
  (params: ContactPageRequest = {}) => fetchContactPage(params),
)

export const filterContacts = createAsyncThunk(
  'contacts/filter',
  (params: ContactPageRequest = {}) => filterContactPage(params),
)

const contactSlice = createSlice({
  name: 'contacts',
  initialState: initialContactState,
  reducers: {},
  extraReducers: (builder) => builder
    .addCase(fetchContacts.pending, (state) => {
      state.loading = true
      state.error = null
    })
    .addCase(fetchContacts.fulfilled, (_state, action) => ({
      ...action.payload,
      loading: false,
      loaded: true,
      error: null,
    }))
    .addCase(fetchContacts.rejected, (state, action) => {
      state.loading = false
      state.error = action.error.message || 'Unable to load contacts'
    })
    .addCase(filterContacts.pending, (state) => {
      state.loading = true
      state.error = null
    })
    .addCase(filterContacts.fulfilled, (_state, action) => ({
      ...action.payload,
      loading: false,
      loaded: true,
      error: null,
    }))
    .addCase(filterContacts.rejected, (state, action) => {
      state.loading = false
      state.error = action.error.message || 'Unable to filter contacts'
    }),
})

export type AppRole = string

export type AccessProfile = {
  dashboardAccess: boolean
  adminAccess: boolean
  contactUpdateAccess: boolean
  gridDownloadAccess: boolean
  displayRole: string
  landingPath: string
}

export const normalizeRoles = (value: unknown): AppRole[] => {
  const raw = Array.isArray(value) ? value : value ? [value] : []
  return Array.from(new Set(raw.map((role) => String(role || '').trim().toUpperCase().replace(/^ROLE_/, '')).filter(Boolean)))
}

export const normalizeAccessProfile = (value: unknown): AccessProfile => {
  const data = (value && typeof value === 'object' ? value : {}) as Record<string, unknown>
  return {
    dashboardAccess: Boolean(data.dashboardAccess),
    adminAccess: Boolean(data.adminAccess),
    contactUpdateAccess: Boolean(data.contactUpdateAccess),
    gridDownloadAccess: Boolean(data.gridDownloadAccess),
    displayRole: String(data.displayRole || 'User'),
    landingPath: String(data.landingPath || '/contacts'),
  }
}

export const readAccessProfile = (): AccessProfile => {
  try {
    return normalizeAccessProfile(JSON.parse(localStorage.getItem('accessProfile') || '{}'))
  }
  catch {
    return normalizeAccessProfile({})
  }
}

const localDate = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

const today = () => localDate(new Date())

const weekAgo = () => {
  const date = new Date()
  date.setDate(date.getDate() - 6)
  return localDate(date)
}

const defaultStoredFilters = (): ContactFilters => ({
  search: '',
  name: '',
  contactType: '',
  department: '',
  city: '',
  status: 'Active',
  fromDate: weekAgo(),
  toDate: today(),
  sortBy: '',
  sortDirection: '',
})

type UiState = {
  draftFilters: ContactFilters
  appliedFilters: ContactFilters
  dropdowns: ContactDropdownData | null
  email: string
  roles: AppRole[]
  access: AccessProfile
}

const readRoles = (): AppRole[] => {
  try {
    const value = JSON.parse(localStorage.getItem('roles') || '[]')
    return normalizeRoles(value)
  }
  catch {
    return []
  }
}

const initialUiState: UiState = {
  draftFilters: defaultStoredFilters(),
  appliedFilters: defaultStoredFilters(),
  dropdowns: null,
  email: localStorage.getItem('loggedInUser') || '',
  roles: readRoles(),
  access: readAccessProfile(),
}

const uiSlice = createSlice({
  name: 'ui',
  initialState: initialUiState,
  reducers: {
    setDraftFilters: (state, action) => {
      state.draftFilters = action.payload
    },
    setAppliedFilters: (state, action) => {
      state.appliedFilters = action.payload
    },
    setDropdownCache: (state, action) => {
      state.dropdowns = action.payload
    },
    setSession: (state, action) => {
      state.email = action.payload.email || ''
      state.roles = normalizeRoles(action.payload.roles)
      state.access = normalizeAccessProfile(action.payload.access)
    },
    resetUiSession: (state) => {
      state.email = ''
      state.roles = []
      state.access = normalizeAccessProfile({})
      state.draftFilters = defaultStoredFilters()
      state.appliedFilters = defaultStoredFilters()
      state.dropdowns = null
    },
  },
})

export const {
  setDraftFilters,
  setAppliedFilters,
  setDropdownCache,
  setSession,
  resetUiSession,
} = uiSlice.actions

export const contactReducer = contactSlice.reducer
export const uiReducer = uiSlice.reducer

export const getContactById = async (
  id: number,
): Promise<ContactDetail> => {
  const response = await axios.get(`${API}/${id}`)
  return ensureSuccess<any>(
    response.data,
    'Unable to fetch contact',
  ).data
}

export const deactivateContact = async (id: number) => {
  const response = await axios.delete(`${API}/${id}`)
  return ensureSuccess<any>(
    response.data,
    'Unable to deactivate contact',
  )
}

export const saveContact = async (
  payload: ContactRequestDto | ContactRequestDto[],
): Promise<{
  message: string
  data: BulkSaveResult
}> => {
  const update = !Array.isArray(payload) && Boolean(payload.id)

  const response = await axios.post(
    `${API}/save`,
    Array.isArray(payload) ? payload : [payload],
  )

  const result = ensureSuccess<any>(
    response.data,
    update ? 'Unable to update contact' : 'Unable to save contacts',
  )

  const data = normalizeSaveResult(result.data)

  if (!Array.isArray(payload) && !data.saved.length)
    throw new Error(
      data.duplicates[0]?.message ||
      data.invalid[0]?.message ||
      (update ? 'Unable to update contact' : 'Unable to save contact'),
    )

  notifyChanged()

  return {
    message:
      result.message ||
      (update ? 'Contact updated successfully' : 'Contacts saved successfully'),
    data,
  }
}

export const getDropdowns = async (): Promise<ContactDropdownData> => {
  const response = await axios.get(`${MASTER_API}/dropdown`)

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load master dropdowns',
  )

  const data = result.data || {}

  const values = (items: any): string[] => {
    if (!Array.isArray(items))
      return []

    const normalized = items
      .map((item: any) => {
        if (typeof item === 'string')
          return item.trim()

        return String(item?.name || '').trim()
      })
      .filter(Boolean)

    return Array.from(new Set(normalized)).sort((a, b) =>
      a.localeCompare(b, undefined, {
        sensitivity: 'base',
      }),
    )
  }

  return {
    names: values(data.names),
    contactTypes: values(data.contactTypes),
    departments: values(data.departments),
    cities: values(data.cities),
    genders: values(data.genders),
    maritalStatuses: values(data.maritalStatuses),
    bloodGroups: values(data.bloodGroups),
    skills: values(data.skills),
    languages: values(data.languages),
    statuses: values(data.statuses).length
      ? values(data.statuses) as ContactStatus[]
      : ['Active', 'Inactive'],
  }
}

export const getNameSuggestions = async (
  query: string,
): Promise<string[]> => {
  if (!query.trim())
    return []

  const response = await axios.get(`${API}/name-suggestions`, {
    params: {
      query,
    },
  })

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load suggestions',
  )

  return Array.isArray(result.data)
    ? result.data
    : []
}

export const getUserNames = async (
  ids: number[],
): Promise<Record<number, string>> => {
  if (!ids.length)
    return {}

  const response = await axios.get(`${USER_API}/names`, {
    params: {
      ids: ids.join(','),
    },
  })

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load user names',
  )

  const data = result.data || {}

  return Object.fromEntries(
    Object.entries(data).map(([id, name]) => [
      Number(id),
      String(name ?? ''),
    ]),
  )
}

export const getStatusCount = async (
  params: ContactSearchParams = {},
): Promise<{
  active: number
  inactive: number
}> => {
  const response = await axios.get(`${API}/status-count`, {
    params,
  })

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load count',
  )

  return {
    active: Number(result.data?.active || 0),
    inactive: Number(result.data?.inactive || 0),
  }
}

export type UploadedContactFile = {
  uuid: string
  fileName: string
  fileType?: 'PHOTO' | 'DOCUMENT' | string
}

export type ContactUploadRequest = {
  file: File
  type: 'PHOTO' | 'DOCUMENT'
}

export type ContactFileDownload = {
  blob: Blob
  fileName: string
  contentType: string
}

const getDownloadFileName = (
  contentDisposition?: string,
  fallback = 'attachment',
) => {
  if (!contentDisposition)
    return fallback

  const utf8Match = contentDisposition.match(
    /filename\*=UTF-8''([^;]+)/i,
  )

  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(
        utf8Match[1].replace(/["']/g, ''),
      )
    }
    catch {
      return utf8Match[1].replace(/["']/g, '')
    }
  }

  const normalMatch = contentDisposition.match(
    /filename="?([^";]+)"?/i,
  )

  return normalMatch?.[1]?.trim() || fallback
}

const validateUploadRequest = ({
  file,
  type,
}: ContactUploadRequest) => {
  if (!file)
    throw new Error('Please select a file')

  if (file.size > 10 * 1024 * 1024)
    throw new Error(`${file.name} exceeds the 10 MB file limit`)

  const extension =
    file.name
      .split('.')
      .pop()
      ?.toLowerCase() || ''

  const allowed = type === 'PHOTO'
    ? [
        'jpg',
        'jpeg',
        'png',
      ]
    : [
        'pdf',
        'doc',
        'docx',
        'xls',
        'xlsx',
      ]

  if (!allowed.includes(extension)) {
    throw new Error(
      type === 'PHOTO'
        ? 'Photo must be JPG, JPEG or PNG'
        : 'Document must be PDF, DOC, DOCX, XLS or XLSX',
    )
  }
}

export const uploadFiles = async (
  requests: ContactUploadRequest[],
): Promise<UploadedContactFile[]> => {
  if (!requests.length)
    return []

  requests.forEach(validateUploadRequest)

  const body = new FormData()

  requests.forEach(({ file, type }) => {
    body.append('files', file, file.name)
    body.append('types', type)
  })

  const response = await axios.post(
    `${API}/file/upload`,
    body,
  )

  const result = ensureSuccess<{
    data: UploadedContactFile[]
  }>(
    response.data,
    'Unable to upload files',
  )

  if (
    !Array.isArray(result.data) ||
    result.data.length !== requests.length
  ) {
    throw new Error(
      'Uploaded file response is incomplete',
    )
  }

  return result.data
}

export const uploadFile = async (
  file: File,
  type: 'PHOTO' | 'DOCUMENT',
): Promise<UploadedContactFile> => {
  const files = await uploadFiles([
    {
      file,
      type,
    },
  ])

  if (!files[0])
    throw new Error('Unable to upload file')

  return files[0]
}

export const deleteContactFile = async (
  uuid: string,
): Promise<void> => {
  if (!uuid)
    return

  const response = await axios.delete(
    `${API}/file/${uuid}`,
  )

  ensureSuccess<any>(
    response.data,
    'Unable to remove file',
  )
}

export const downloadContactFile = async (
  uuid: string,
): Promise<ContactFileDownload> => {
  if (!uuid)
    throw new Error('File UUID is required')

  const response = await axios.get(
    `${API}/file/${uuid}`,
    {
      responseType: 'blob',
    },
  )

  const contentType =
    response.headers['content-type'] ||
    response.data?.type ||
    'application/octet-stream'

  return {
    blob: response.data,
    fileName: getDownloadFileName(
      response.headers['content-disposition'],
      `attachment-${uuid}`,
    ),
    contentType,
  }
}

export const getFileUrl = (uuid: string) =>
  `${API}/file/${uuid}`

export const validateContactsExcel = async (
  file: File,
): Promise<{
  message: string
  data: ExcelValidationResult
}> => {
  const body = new FormData()
  body.append('file', file)

  const response = await axios.post(
    `/contact/excel/validate`,
    body,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Invalid Excel file',
  )

  const data = normalizeExcelValidation(
    result.data || {},
  )

  return {
    message:
      result.message ||
      `New: ${data.newCount}, Update: ${data.updateCount}, Duplicate: ${data.duplicateCount}, Incorrect: ${data.invalidCount}`,
    data,
  }
}

export const importContactsExcel = async (
  file: File,
): Promise<{
  message: string
  data: ExcelImportResult
}> => {
  const body = new FormData()
  body.append('file', file)

  const response = await axios.post(
    `/contact/excel/import`,
    body,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to import Excel',
  )

  const data = normalizeSaveResult(result.data)

  notifyChanged()

  return {
    message:
      result.message ||
      'Excel import completed',
    data: {
      ...data,
      totalRows: Number(
        result.data?.totalRows || 0,
      ),
    },
  }
}

export type ContactTableColumnConfig = {
  key: string
  header: string
  visible?: boolean
  order?: number
  size?: number
  sortable?: boolean
  renderer?: string
}

export const getContactTableConfig = async (): Promise<
  ContactTableColumnConfig[]
> => {
  const response = await axios.get(
    `/contact/table-config/CONTACT`,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load contact table config',
  )

  const raw = result?.data

  const rows = Array.isArray(raw)
    ? raw
    : Array.isArray(raw?.columns)
      ? raw.columns
      : Array.isArray(raw?.content)
        ? raw.content
        : []

  return rows
    .map((item: any, index: number) => ({
      key: String(
        item?.key ??
        item?.field ??
        item?.columnKey ??
        item?.accessorKey ??
        item?.name ??
        '',
      ).trim(),

      header: String(
        item?.header ??
        item?.label ??
        item?.displayName ??
        item?.title ??
        item?.columnName ??
        '',
      ).trim(),

      visible:
        item?.visible ??
        item?.isVisible ??
        item?.enabled ??
        true,

      order: Number(
        item?.order ??
        item?.displayOrder ??
        item?.sequence ??
        item?.position ??
        index,
      ),

      size:
        Number(
          item?.size ??
          item?.width ??
          0,
        ) || undefined,

      sortable:
        item?.sortable ??
        item?.enableSorting ??
        true,

      renderer:
        String(item?.renderer ?? '').trim() ||
        undefined,
    }))
    .filter(
      (item: ContactTableColumnConfig) => item.key,
    )
}

export const downloadContactFormat = async (): Promise<Blob> => {
  const response = await axios.get(
    `${API}/excel/format`,
    {
      responseType: 'blob',
    },
  )

  return response.data
}

export const downloadContactsExcel = async (
  params: ContactSearchParams = {},
): Promise<Blob> => {
  const response = await axios.get(
    `${API}/excel/download`,
    {
      params,
      responseType: 'blob',
    },
  )

  return response.data
}
export const requestContactsExcelDownload = async (
  params: ContactSearchParams = {},
): Promise<{
  message: string
  requestId: number
}> => {
  const response = await axios.post(
    `${API}/excel/download-request`,
    {
      filters: params,
    },
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to create download approval request',
  )

  window.dispatchEvent(
    new Event('notification-updated'),
  )

  return {
    message:
      result.message ||
      'Download request sent to admin for approval',
    requestId: Number(
      result.data?.requestId || 0,
    ),
  }
}

export const approveExcelDownloadRequest = async (
  requestId: number,
): Promise<string> => {
  const response = await axios.post(
    `${API}/excel/download-request/${requestId}/approve`,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to approve download request',
  )

  window.dispatchEvent(
    new Event('notification-updated'),
  )

  return (
    result.message ||
    'Download request approved'
  )
}

export const rejectExcelDownloadRequest = async (
  requestId: number,
): Promise<string> => {
  const response = await axios.post(
    `${API}/excel/download-request/${requestId}/reject`,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to reject download request',
  )

  window.dispatchEvent(
    new Event('notification-updated'),
  )

  return (
    result.message ||
    'Download request rejected'
  )
}

export const getContactHistory = async (
  contactId: number,
): Promise<ContactHistoryItem[]> => {
  const response = await axios.get(
    `${API}/history/${contactId}`,
  )

  const payload = response.data

  if (
    String(payload?.status || '').toUpperCase() !==
    'SUCCESS'
  ) {
    throw new Error(
      payload?.error ||
      payload?.message ||
      'Unable to load contact history',
    )
  }

  return Array.isArray(payload?.data)
    ? payload.data
    : []
}

let contactHistoryTableConfigCache: ContactTableColumnConfig[] | null = null
let contactHistoryTableConfigPromise: Promise<ContactTableColumnConfig[]> | null = null

export const getContactHistoryTableConfig = async (): Promise<
  ContactTableColumnConfig[]
> => {
  if (contactHistoryTableConfigCache) {
    return contactHistoryTableConfigCache
  }

  if (contactHistoryTableConfigPromise) {
    return contactHistoryTableConfigPromise
  }

  contactHistoryTableConfigPromise = (async () => {
    const response = await axios.get(
      `${API}/history-config`,
    )

    const result = ensureSuccess<any>(
      response.data,
      'Unable to load history table config',
    )

    const rows = Array.isArray(result.data)
      ? result.data
      : []

    const config = rows
      .map((item: any, index: number) => ({
        key: String(
          item?.key ?? '',
        ).trim(),

        header: String(
          item?.header ?? '',
        ).trim(),

        visible:
          item?.visible ??
          true,

        order: Number(
          item?.order ??
          index,
        ),

        size:
          Number(
            item?.size ??
            0,
          ) || undefined,

        sortable:
          item?.sortable ??
          false,

        renderer:
          String(
            item?.renderer ?? '',
          ).trim() || undefined,
      }))
      .filter(
        (item: ContactTableColumnConfig) =>
          item.key &&
          item.visible !== false,
      )
      .sort(
        (
          first: ContactTableColumnConfig,
          second: ContactTableColumnConfig,
        ) =>
          Number(first.order ?? 0) -
          Number(second.order ?? 0),
      )

    contactHistoryTableConfigCache = config
    return config
  })()

  try {
    return await contactHistoryTableConfigPromise
  }
  finally {
    contactHistoryTableConfigPromise = null
  }
}

export const getNotifications = async (
  page = 0,
  size = 5,
): Promise<NotificationData> => {
  const response = await axios.get(
    `/contact/notifications`,
    {
      params: {
        page,
        size,
      },
    },
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to load notifications',
  )

  return {
    items: Array.isArray(
      result.data?.items,
    )
      ? result.data.items
      : [],

    unreadCount: Number(
      result.data?.unreadCount || 0,
    ),

    page: Number(
      result.data?.page || 0,
    ),

    size: Number(
      result.data?.size || size,
    ),

    totalElements: Number(
      result.data?.totalElements || 0,
    ),

    totalPages: Number(
      result.data?.totalPages || 0,
    ),
  }
}

export const markNotificationRead = async (
  id: number,
): Promise<number> => {
  const response = await axios.patch(
    `/contact/notifications/${id}/read`,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to mark notification as read',
  )

  return Number(
    result.data?.unreadCount ?? 0,
  )
}

export const deleteNotification = async (
  id: number,
): Promise<number> => {
  const response = await axios.delete(
    `/contact/notifications/${id}`,
  )

  const result = ensureSuccess<any>(
    response.data,
    'Unable to delete notification',
  )

  return Number(
    result.data?.unreadCount ?? 0,
  )
}

export const downloadNotificationAttachment = async (
  notification: AppNotification,
) => {
  if (!notification?.id)
    throw new Error('Attachment is unavailable')

  const response = await axios.get(
    `/contact/notifications/${notification.id}/attachment`,
    {
      responseType: 'blob',
    },
  )

  const contentDisposition =
    response.headers['content-disposition']

  const fileName = getDownloadFileName(
    contentDisposition,
    notification.attachmentName ||
    `attachment-${notification.id}`,
  )

  const blob = new Blob(
    [response.data],
    {
      type:
        response.headers['content-type'] ||
        response.data?.type ||
        'application/octet-stream',
    },
  )

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = fileName

  document.body.appendChild(link)
  link.click()
  link.remove()

  window.setTimeout(
    () => URL.revokeObjectURL(url),
    1000,
  )
}

export type ChartPoint = {
  label: string
  value: number
}

export type StackedChartPoint = {
  label: string
  active: number
  inactive: number
}

export type ContactAnalytics = {
  total: number
  active: number
  inactive: number
  contactTypes: ChartPoint[]
  contactTypeStatus: StackedChartPoint[]
  departments: ChartPoint[]
  departmentStatus: StackedChartPoint[]
  cities: ChartPoint[]
}

export const getContactAnalytics = async (
  params: ContactSearchParams = {},
): Promise<ContactAnalytics> => {
  const response = await axios.get(
    `${API}/analytics`,
    {
      params,
    },
  )

  return ensureSuccess<any>(
    response.data,
    'Unable to load analytics',
  ).data
}

export const sendContactEmail = async (
  body: FormData,
) => {
  const response = await axios.post(
    `/contact/email/send`,
    body,
  )

  return ensureSuccess<any>(
    response.data,
    'Unable to send email',
  )
}