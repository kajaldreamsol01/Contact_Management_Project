import { array, boolean, mixed, object, string, type StringSchema } from 'yup'
const optional = (schema: StringSchema) => schema
  .transform((value, original) => original === '' || original === null || original === undefined ? undefined : value)
  .notRequired()
const mobile = /^[6-9]\d{9}$/
const contactSchema = object({
  contactType: string()
    .trim()
    .required('Contact Type is required'),
  name: string()
    .trim()
    .required('Name is required')
    .matches(/^[A-Za-z ]+$/, 'Name should contain alphabets only')
    .max(150, 'Name cannot exceed 150 characters'),
  communicationName: string()
    .trim()
    .max(150, 'Communication Name cannot exceed 150 characters'),
  department: string()
    .trim()
    .max(100, 'Department cannot exceed 100 characters'),
  designation: string()
    .trim()
    .max(100, 'Designation cannot exceed 100 characters'),
  companyName: string()
    .trim()
    .max(150, 'Company Name cannot exceed 150 characters'),
  mobile: string()
    .trim()
    .required('Mobile Number is required')
    .matches(mobile, 'Enter valid 10 digit mobile number'),
  alternateMobile: optional(string().trim())
    .matches(mobile, {
      message: 'Enter valid 10 digit alternate mobile number',
      excludeEmptyString: true,
    })
    .test('different-mobile', 'Alternate mobile must be different from mobile number', function(value) {
      return !value || value !== this.parent.mobile
    }),
  officeNumber: optional(string().trim()).matches(/^\d{1,20}$/, {
    message: 'Office Number should contain numbers only',
    excludeEmptyString: true,
  }),
  email: string()
    .trim()
    .required('Email ID is required')
    .email('Enter valid Email ID')
    .max(150, 'Email cannot exceed 150 characters'),
  alternateEmail: optional(string().trim())
    .email('Enter valid Alternate Email')
    .max(150, 'Alternate Email cannot exceed 150 characters')
    .test('different-email', 'Alternate Email must be different from Email ID', function(value) {
      return (!value ||
        value.toLowerCase() !==
        String(this.parent.email || '').toLowerCase())
    }),
  employeeId: optional(string().trim())
    .matches(/^[A-Za-z0-9]+$/, {
      message: 'Employee ID must be alphanumeric',
      excludeEmptyString: true,
    })
    .max(30, 'Employee ID cannot exceed 30 characters'),
  gender: string()
    .trim()
    .oneOf(['', 'Male', 'Female', 'Other'], 'Select valid gender'),
  maritalStatus: string()
    .trim(),
  dateOfBirth: optional(string()).test('adult', 'Applicant must be at least 18 years old', (value) => {
    if (!value)
      return true
    const date = new Date(value)
    if (Number.isNaN(date.getTime()))
      return false
    const limit = new Date()
    limit.setFullYear(limit.getFullYear() - 18)
    return date <= limit
  }),
  anniversaryDate: optional(string()).test('not-future', 'Anniversary cannot be in the future', (value) => !value || new Date(value) <= new Date()),
  bloodGroup: string()
    .trim(),
  country: string()
    .trim()
    .max(100),
  state: string()
    .trim()
    .max(100),
  city: string()
    .trim()
    .max(100),
  address: string()
    .trim()
    .max(1000, 'Address cannot exceed 1000 characters'),
  pinCode: optional(string().trim()).matches(/^\d{6}$/, {
    message: 'Pin Code must be 6 digits',
    excludeEmptyString: true,
  }),
  skills: array()
    .of(string().trim()),
  languages: array()
    .of(string().trim()),
  emergencyContactName: string()
    .trim()
    .max(150),
  emergencyContactNumber: optional(string().trim()).matches(mobile, {
    message: 'Enter valid emergency contact number',
    excludeEmptyString: true,
  }),
  remarks: string()
    .trim()
    .max(1000, 'Remarks cannot exceed 1000 characters'),
  status: boolean().required(),
  photoUuid: string(),
  documentUuids: array()
    .of(string().defined()),
  photo: mixed<File>()
    .nullable(),
  documents: array()
    .of(mixed<File>().defined()),
})
export default contactSchema
export const contactArraySchema = object({
  contacts: array()
    .of(contactSchema)
    .min(1, 'At least one contact is required')
    .required(),
})
