import { useState } from "react";
import { useFieldArray, useForm, type Path, type Resolver } from "react-hook-form";
import { yupResolver } from "@hookform/resolvers/yup";
import { Alert, Box, Button, Grid, IconButton, Paper, Typography } from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import { contactArraySchema } from "../../../../validation/contactSchema";
import { uploadFiles, type BulkSaveResult, type ContactDropdownData, type ContactRequestDto, type ContactUploadRequest } from "./apis";
import InputField from "../InputField";
import AutoCompleteField from "../AutoCompleteField";
import MultiSelectAutoCompleteField from "../MultiSelectAutoCompleteField";
export type ContactFormData = ContactRequestDto & {
    photo: File | null;
    documents: File[];
};
type AddFormData = {
    contacts: ContactFormData[];
};
type Props = {
    dropdowns: ContactDropdownData;
    onSave: (payload: ContactRequestDto[]) => Promise<{
        message: string;
        data: BulkSaveResult;
    }>;
    onClose: () => void;
};
export const emptyContact = (): ContactFormData => ({
    contactCode: "",
    contactType: "",
    name: "",
    communicationName: "",
    department: "",
    designation: "",
    companyName: "",
    mobile: "",
    alternateMobile: "",
    officeNumber: "",
    email: "",
    alternateEmail: "",
    employeeId: "",
    gender: "",
    maritalStatus: "",
    dateOfBirth: "",
    anniversaryDate: "",
    bloodGroup: "",
    country: "",
    state: "",
    city: "",
    address: "",
    pinCode: "",
    skills: [],
    languages: [],
    emergencyContactName: "",
    emergencyContactNumber: "",
    remarks: "",
    status: false,
    photoUuid: "",
    documentUuids: [],
    photo: null,
    documents: [],
});
const sectionSx = { p: 2, mb: 1.5, border: "1px solid #dce8e6", borderRadius: 2, boxShadow: "none" };
const titleSx = { mb: 1.5, pb: 0.75, fontSize: 14, fontWeight: 700, color: "#007f75", borderBottom: "1px solid #e5eeee" };
const today=()=>{const date=new Date();const offset=date.getTimezoneOffset();return new Date(date.getTime()-offset*60000).toISOString().slice(0,10);};
const prepareContactPayload = async (contacts: ContactFormData[]): Promise<ContactRequestDto[]> => {
    const uploadRequests: ContactUploadRequest[] = [];
    const uploadTargets: {
        contactIndex: number;
        kind: "photo" | "document";
    }[] = [];
    contacts.forEach((form, contactIndex) => {
        if (form.photo) {
            uploadRequests.push({ file: form.photo, type: "PHOTO" });
            uploadTargets.push({ contactIndex, kind: "photo" });
        }
        (form.documents || []).forEach((file) => {
            uploadRequests.push({ file, type: "DOCUMENT" });
            uploadTargets.push({ contactIndex, kind: "document" });
        });
    });
    const uploadedFiles = uploadRequests.length
        ? await uploadFiles(uploadRequests)
        : [];
    const photoUuids = contacts.map((form) => form.photoUuid || "");
    const documentUuids = contacts.map((form) => [...(form.documentUuids || [])]);
    uploadedFiles.forEach((uploaded, index) => {
        const target = uploadTargets[index];
        if (!target)
            return;
        if (target.kind === "photo")
            photoUuids[target.contactIndex] = uploaded.uuid;
        else
            documentUuids[target.contactIndex].push(uploaded.uuid);
    });
    return contacts.map((form, index) => {
        const { photo: _photo, documents: _documents, ...dto } = form;
        return {
            ...dto,
            photoUuid: photoUuids[index],
            documentUuids: documentUuids[index],
        };
    });
};
function AddContactForm({ dropdowns, onSave, onClose }: Props) {
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [validationMessage, setValidationMessage] = useState("");
    const { control, handleSubmit, reset, setValue, trigger, watch, formState: { errors }, } = useForm<AddFormData>({
        resolver: yupResolver(contactArraySchema) as unknown as Resolver<AddFormData>,
        defaultValues:{contacts:[emptyContact()]},
        mode:"onBlur",
        reValidateMode:"onChange",
    });
    const { fields, append, remove } = useFieldArray({ control, name: "contacts" });
    const watchedContacts = watch("contacts");
    const name = (index: number, key: keyof ContactFormData) => `contacts.${index}.${key}` as Path<AddFormData>;
    const fieldError = (index: number, key: keyof ContactFormData) => errors.contacts?.[index]?.[key]?.message as string | undefined;
    const text=(index:number,key:keyof ContactFormData,label:string,options?:{required?:boolean;type?:string;multiline?:boolean;rows?:number;max?:string})=>(<InputField name={name(index,key)} control={control} label={label} type={options?.type} multiline={options?.multiline} rows={options?.rows} max={options?.max} isRequired={options?.required} error={Boolean(fieldError(index,key))} errorMessage={fieldError(index,key)}/>);
    const select = (index: number, key: keyof ContactFormData, label: string, options: string[], required = false) => (<AutoCompleteField name={name(index, key)} control={control} label={label} options={options} isRequired={required} error={Boolean(fieldError(index, key))} errorMessage={fieldError(index, key)}/>);
    const addOrEditDetails = async ({ contacts }: AddFormData) => {
        setSaving(true);
        setError("");
        setValidationMessage("");
        try {
            const maxDate=today();
            if(contacts.some((contact)=>contact.dateOfBirth&&contact.dateOfBirth>maxDate)){setError("Date Of Birth cannot be a future date.");return;}
            if(contacts.some((contact)=>contact.anniversaryDate&&contact.anniversaryDate>maxDate)){setError("Anniversary Date cannot be a future date.");return;}
            const payload = await prepareContactPayload(contacts);
            const result = await onSave(payload);
            if (result.data.duplicateCount || result.data.invalidCount) {
                setError(result.message);
                return;
            }
            reset({ contacts: [emptyContact()] });
            onClose();
        }
        catch (error) {
            setError(error instanceof Error ? error.message : "Unable to save contacts");
        }
        finally {
            setSaving(false);
        }
    };
    const addAnother = async () => {
        const index = fields.length - 1;
        const valid = await trigger(`contacts.${index}` as `contacts.${number}`);
        if (!valid) {
            setValidationMessage("Please fill all mandatory fields properly before adding another contact.");
            return;
        }
        setValidationMessage("");
        append(emptyContact());
    };
    const handleInvalidSave = () => {
        setValidationMessage("Please fill all mandatory fields properly before saving the form.");
    };
    return (<Box sx={{ p: 1 }}>
      <form onSubmit={handleSubmit(addOrEditDetails, handleInvalidSave)} noValidate>
      <Box>
      {error && <Alert severity="error" sx={{ mb: 1.5 }}>{error}</Alert>}

      {fields.map((field, index) => (<Paper key={field.id} variant="outlined" sx={{ mb: 2, p: 2, borderColor: "#cfe5e2" }}>
          <Box sx={{ display: "flex", alignItems: "center", mb: 1.5 }}>
            <Typography sx={{ flex: 1, fontWeight: 700, color: "#0f766e" }}>Contact {index + 1}</Typography>
            {fields.length > 1 && (<IconButton color="error" onClick={() => remove(index)}><DeleteOutlineOutlinedIcon /></IconButton>)}
          </Box>

          <Paper elevation={0} sx={sectionSx}>
            <Typography sx={titleSx}>Basic Information</Typography>
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 6 }}>{select(index, "contactType", "Contact Type", dropdowns.contactTypes, true)}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "name", "Name", { required: true })}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "communicationName", "Communication Name")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "department", "Department")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "designation", "Designation")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "companyName", "Company Name")}</Grid>
            </Grid>
          </Paper>

          <Paper elevation={0} sx={sectionSx}>
            <Typography sx={titleSx}>Contact Information</Typography>
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "mobile", "Mobile", { required: true })}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "alternateMobile", "Alternate Mobile")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "officeNumber", "Office Number")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "email", "Email", { required: true })}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "alternateEmail", "Alternate Email")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "employeeId", "Employee ID")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{select(index, "gender", "Gender", dropdowns.genders)}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{select(index, "maritalStatus", "Marital Status", dropdowns.maritalStatuses)}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index,"dateOfBirth","Date Of Birth",{type:"date",max:today()})}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index,"anniversaryDate","Anniversary Date",{type:"date",max:today()})}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{select(index, "bloodGroup", "Blood Group", dropdowns.bloodGroups)}</Grid>
            </Grid>
          </Paper>

          <Paper elevation={0} sx={sectionSx}>
            <Typography sx={titleSx}>Address & Other Details</Typography>
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 4 }}>{text(index, "country", "Country")}</Grid>
              <Grid size={{ xs: 12, sm: 4 }}>{text(index, "state", "State")}</Grid>
              <Grid size={{ xs: 12, sm: 4 }}>{text(index, "city", "City")}</Grid>
              <Grid size={{ xs: 12, sm: 8 }}>{text(index, "address", "Address")}</Grid>
              <Grid size={{ xs: 12, sm: 4 }}>{text(index, "pinCode", "Pin Code")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <MultiSelectAutoCompleteField name={name(index, "skills")} control={control} label="Skills" options={dropdowns.skills} error={Boolean(fieldError(index, "skills"))} errorMessage={fieldError(index, "skills")}/>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <MultiSelectAutoCompleteField name={name(index, "languages")} control={control} label="Languages" options={dropdowns.languages} error={Boolean(fieldError(index, "languages"))} errorMessage={fieldError(index, "languages")}/>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "emergencyContactName", "Emergency Contact Name")}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>{text(index, "emergencyContactNumber", "Emergency Contact Number")}</Grid>
              <Grid size={{ xs: 12 }}>{text(index, "remarks", "Remarks", { multiline: true, rows: 3 })}</Grid>
            </Grid>
          </Paper>

          <Paper elevation={0} sx={sectionSx}>
            <Typography sx={titleSx}>Attachments</Typography>
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Button variant="outlined" component="label" fullWidth disabled={saving}>
                  {watchedContacts?.[index]?.photo?.name || "Select Photo"}
                  <input hidden type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" onChange={(event) => {
                const file = event.target.files?.[0] || null;
                setValue(name(index, "photo"), file, { shouldDirty: true, shouldValidate: false });
                event.target.value = "";
            }}/>
                </Button>
                {watchedContacts?.[index]?.photo && (<Box sx={{ mt: 0.75, display: "flex", alignItems: "center", gap: 1 }}>
                    <Typography variant="caption" sx={{ flex: 1, wordBreak: "break-all" }}>
                      Photo: {watchedContacts[index].photo?.name}
                    </Typography>
                    <Button type="button" size="small" color="error" onClick={() => setValue(name(index, "photo"), null, { shouldDirty: true })}>
                      Remove
                    </Button>
                  </Box>)}
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Button variant="outlined" component="label" fullWidth disabled={saving}>
                  {watchedContacts?.[index]?.documents?.length
                ? `${watchedContacts[index].documents.length} document(s) selected`
                : "Select Documents"}
                  <input hidden type="file" multiple accept=".pdf,.doc,.docx,.xls,.xlsx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => {
                const files = Array.from(event.target.files || []);
                setValue(name(index, "documents"), files, { shouldDirty: true, shouldValidate: false });
                event.target.value = "";
            }}/>
                </Button>
                {Boolean(watchedContacts?.[index]?.documents?.length) && (<Box sx={{ mt: 0.75 }}>
                    {watchedContacts[index].documents.map((file) => (<Typography key={`${file.name}-${file.lastModified}`} variant="caption" sx={{ display: "block", wordBreak: "break-all" }}>
                        {file.name}
                      </Typography>))}
                    <Button type="button" size="small" color="error" onClick={() => setValue(name(index, "documents"), [], { shouldDirty: true })}>
                      Clear documents
                    </Button>
                  </Box>)}
              </Grid>
            </Grid>
          </Paper>
        </Paper>))}

      <Box sx={{ display: "flex", justifyContent: "center", mb: 2 }}>
        <Button type="button" variant="outlined" startIcon={<AddOutlinedIcon />} disabled={saving} onClick={addAnother}>
          Add Another Contact
        </Button>
      </Box>

      {validationMessage && (<Alert severity="warning" sx={{ mb: 1.5, fontWeight: 600 }}>
          {validationMessage}
        </Alert>)}

      <Box sx={{ display: "flex", justifyContent: "center", gap: 1.5, flexWrap: "wrap" }}>
        <Button type="submit" variant="contained" startIcon={<SaveOutlinedIcon />} disabled={saving}>
          {saving ? "Saving..." : `Save ${fields.length} Contact${fields.length > 1 ? "s" : ""}`}
        </Button>
        <Button type="button" variant="outlined" startIcon={<RestartAltOutlinedIcon />} disabled={saving} onClick={() => { reset({ contacts: [emptyContact()] }); setValidationMessage(""); setError(""); }}>
          Reset
        </Button>
        <Button type="button" variant="outlined" color="error" startIcon={<CloseOutlinedIcon />} disabled={saving} onClick={onClose}>
          Cancel
        </Button>
      </Box>
      </Box>
      </form>
    </Box>);
}
export default AddContactForm;
