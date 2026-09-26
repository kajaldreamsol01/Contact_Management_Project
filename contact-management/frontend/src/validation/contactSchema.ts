import * as yup from 'yup'
const optional = (schema: yup.StringSchema) => schema
  .transform((value, original) => original === '' || original == null ? undefined : value)
  .notRequired()
const mobile = /^[6-9]\d{9}$/
const contactSchema = yup.object({
  contactType: yup
    .string()
    .trim()
    .required('Contact Type is required'),
  name: yup
    .string()
    .trim()
    .required('Name is required')
    .matches(/^[A-Za-z ]+$/, 'Name should contain alphabets only')
    .max(150, 'Name cannot exceed 150 characters'),
  communicationName: yup
    .string()
    .trim()
    .max(150, 'Communication Name cannot exceed 150 characters'),
  department: yup
    .string()
    .trim()
    .max(100, 'Department cannot exceed 100 characters'),
  designation: yup
    .string()
    .trim()
    .max(100, 'Designation cannot exceed 100 characters'),
  companyName: yup
    .string()
    .trim()
    .max(150, 'Company Name cannot exceed 150 characters'),
  mobile: yup
    .string()
    .trim()
    .required('Mobile Number is required')
    .matches(mobile, 'Enter valid 10 digit mobile number'),
  alternateMobile: optional(yup.string().trim())
    .matches(mobile, {
      message: 'Enter valid 10 digit alternate mobile number',
      excludeEmptyString: true,
    })
    .test('different-mobile', 'Alternate mobile must be different from mobile number', function(value) {
      return !value || value !== this.parent.mobile
    }),
  officeNumber: optional(yup.string().trim()).matches(/^\d{1,20}$/, {
    message: 'Office Number should contain numbers only',
    excludeEmptyString: true,
  }),
  email: yup
    .string()
    .trim()
    .required('Email ID is required')
    .email('Enter valid Email ID')
    .max(150, 'Email cannot exceed 150 characters'),
  alternateEmail: optional(yup.string().trim())
    .email('Enter valid Alternate Email')
    .max(150, 'Alternate Email cannot exceed 150 characters')
    .test('different-email', 'Alternate Email must be different from Email ID', function(value) {
      return (!value ||
        value.toLowerCase() !==
        String(this.parent.email || '').toLowerCase())
    }),
  employeeId: optional(yup.string().trim())
    .matches(/^[A-Za-z0-9]+$/, {
      message: 'Employee ID must be alphanumeric',
      excludeEmptyString: true,
    })
    .max(30, 'Employee ID cannot exceed 30 characters'),
  gender: yup
    .string()
    .trim()
    .oneOf(['', 'Male', 'Female', 'Other'], 'Select valid gender'),
  maritalStatus: yup
    .string()
    .trim(),
  dateOfBirth: optional(yup.string()).test('adult', 'Applicant must be at least 18 years old', (value) => {
    if (!value)
      return true
    const date = new Date(value)
    if (Number.isNaN(date.getTime()))
      return false
    const limit = new Date()
    limit.setFullYear(limit.getFullYear() - 18)
    return date <= limit
  }),
  anniversaryDate: optional(yup.string()).test('not-future', 'Anniversary cannot be in the future', (value) => !value || new Date(value) <= new Date()),
  bloodGroup: yup
    .string()
    .trim(),
  country: yup
    .string()
    .trim()
    .max(100),
  state: yup
    .string()
    .trim()
    .max(100),
  city: yup
    .string()
    .trim()
    .max(100),
  address: yup
    .string()
    .trim()
    .max(1000, 'Address cannot exceed 1000 characters'),
  pinCode: optional(yup.string().trim()).matches(/^\d{6}$/, {
    message: 'Pin Code must be 6 digits',
    excludeEmptyString: true,
  }),
  skills: yup
    .array()
    .of(yup.string().trim()),
  languages: yup
    .array()
    .of(yup.string().trim()),
  emergencyContactName: yup
    .string()
    .trim()
    .max(150),
  emergencyContactNumber: optional(yup.string().trim()).matches(mobile, {
    message: 'Enter valid emergency contact number',
    excludeEmptyString: true,
  }),
  remarks: yup
    .string()
    .trim()
    .max(1000, 'Remarks cannot exceed 1000 characters'),
  status: yup.boolean().required(),
  photoUuid: yup.string(),
  documentUuids: yup
    .array()
    .of(yup.string().defined()),
  photo: yup
    .mixed<File>()
    .nullable(),
  documents: yup
    .array()
    .of(yup.mixed<File>().defined()),
})
export default contactSchema
export const contactArraySchema = yup.object({
  contacts: yup
    .array()
    .of(contactSchema)
    .min(1, 'At least one contact is required')
    .required(),
})
