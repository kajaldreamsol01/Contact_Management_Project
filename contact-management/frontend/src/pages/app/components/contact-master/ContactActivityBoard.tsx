import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Drawer, Fab, IconButton, Menu, MenuItem, Tooltip, Paper, Snackbar, Typography, } from "@mui/material";
import type { MRT_ColumnDef, MRT_PaginationState, MRT_Row, MRT_SortingState, } from "material-react-table";
import ReactTable from "../ReactTable";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import UploadOutlinedIcon from "@mui/icons-material/UploadOutlined";
import ContactActivityFilter, { getAppliedFilterLabels, getDefaultContactFilters, } from "./ContactActivityFilter";
import AddContactForm from "./AddContactForm";
import UpdateContactForm from "./UpdateContactForm";
import ContactHistoryDialog from "./ContactHistoryDialog";
import { deactivateContact, deleteContactFile, downloadContactFile, downloadContactFormat, downloadContactsExcel, requestContactsExcelDownload, getContactTableConfig, getDropdowns, getStatusCount, getUserNames, importContactsExcel, validateContactsExcel, saveContact, filterContacts, setDraftFilters as setDraftFiltersAction, setAppliedFilters as setAppliedFiltersAction, setDropdownCache, type ContactDetail, type ContactDropdownData, type ContactFilters, type ContactListItem, type ContactTableColumnConfig, type ContactRequestDto, type ExcelPreviewRow, } from "./apis";
import { useAppDispatch, useAppSelector } from "../../../../store/hooks";
const EMPTY_DROPDOWNS: ContactDropdownData = {
    names: [],
    contactTypes: [],
    departments: [],
    cities: [],
    genders: [],
    maritalStatuses: [],
    bloodGroups: [],
    skills: [],
    languages: [],
    statuses: ["Active", "Inactive"],
};
const displayValue = (value: unknown) => {
    if (Array.isArray(value))
        return value.length ? value.join(", ") : "N/A";
    const text = String(value ?? "").trim();
    return text || "N/A";
};
const blurActiveElement = () => {
    const activeElement = document.activeElement;
    if (activeElement instanceof HTMLElement)
        activeElement.blur();
};
const formatDateTime = (value?: string | null) => {
    if (!value)
        return "N/A";
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return "N/A";
    const dd = String(date.getDate()).padStart(2, "0");
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const yy = String(date.getFullYear()).slice(-2);
    const hh = String(date.getHours()).padStart(2, "0");
    const min = String(date.getMinutes()).padStart(2, "0");
    const sec = String(date.getSeconds()).padStart(2, "0");
    return `${dd}/${mm}/${yy} ${hh}:${min}:${sec}`;
};
const toApiFilters = (filters: ContactFilters) => ({
    search: filters.search.trim() || undefined,
    name: filters.name || undefined,
    contactType: filters.contactType || undefined,
    department: filters.department || undefined,
    city: filters.city || undefined,
    status: filters.status === "Active" ? false : filters.status === "Inactive" ? true : undefined,
    fromDate: filters.fromDate || undefined,
    toDate: filters.toDate || undefined,
});
const isDefaultOneWeekRange = (filters: ContactFilters) => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 6);
    const text = (d: Date) => d.toISOString().slice(0, 10);
    return filters.fromDate === text(from) && filters.toDate === text(to);
};
type AttachmentFile = {
    uuid: string;
    fileName?: string;
};
function PhotoDownloadCell({ uuid, fileName, onDownload, }: {
    uuid: string;
    fileName?: string;
    onDownload: (uuid: string) => void | Promise<void>;
}) {
    const name = fileName || "Photo";
    return (<Tooltip title={name} arrow>
      <IconButton size="small" aria-label={`Download ${name}`} onClick={() => void onDownload(uuid)} sx={{ color: "#0f9187" }}>
        <DownloadOutlinedIcon fontSize="small"/>
      </IconButton>
    </Tooltip>);
}
function DocumentsDownloadCell({ uuids, files, onDownload, }: {
    uuids: string[];
    files?: AttachmentFile[];
    onDownload: (uuid: string) => void | Promise<void>;
}) {
    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
    if (!uuids.length)
        return <>N/A</>;
    const items = uuids.map((uuid, index) => {
        const file = files?.find((item) => item.uuid === uuid);
        return {
            uuid,
            fileName: file?.fileName || `Document ${index + 1}`,
        };
    });
    return (<>
      <Tooltip title={items.length === 1
            ? items[0].fileName
            : `${items.length} documents - click to choose`} arrow>
        <Button type="button" size="small" variant="text" onClick={(event) => setAnchorEl(event.currentTarget)} startIcon={<DownloadOutlinedIcon fontSize="small"/>} sx={{ minWidth: 0, px: 0.75, textTransform: "none", color: "#0f9187", fontWeight: 700, }}>
          {items.length > 1 ? items.length : ""}
        </Button>
      </Tooltip>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)} slotProps={{
            paper: {
                sx: {
                    maxWidth: 360,
                    maxHeight: 320,
                },
            },
        }}>
        {items.map((item) => (<MenuItem key={item.uuid} onClick={() => {
                setAnchorEl(null);
                void onDownload(item.uuid);
            }} sx={{ gap: 1, maxWidth: 360 }}>
            <DownloadOutlinedIcon fontSize="small" sx={{ color: "#0f9187" }}/>
            <Typography variant="body2" title={item.fileName} sx={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", }}>
              {item.fileName}
            </Typography>
          </MenuItem>))}
      </Menu>
    </>);
}
function ContactActivityBoard() {
    const dispatch = useAppDispatch();
    const { content: contacts, loading, error: contactError, totalElements, } = useAppSelector((state) => state.contacts);
    const { draftFilters, appliedFilters, dropdowns: storedDropdowns, roles, } = useAppSelector((state) => state.ui);
    const isAdmin = roles.includes("ADMIN");
    const isManagement = roles.includes("MANAGEMENT");
    const canUpdate = isAdmin || isManagement;
    const canDownloadGrid = roles.some((role) => ["ADMIN", "HOD", "MANAGEMENT", "USER"].includes(role));
    const masterDropdowns = storedDropdowns ?? EMPTY_DROPDOWNS;
    const [filterDropdowns, setFilterDropdowns] = useState<ContactDropdownData>(EMPTY_DROPDOWNS);
    const [pagination, setPagination] = useState<MRT_PaginationState>({
        pageIndex: 0,
        pageSize: 10,
    });
    const [tableResetKey, setTableResetKey] = useState(0);
    const [recentlyUpdatedId, setRecentlyUpdatedId] = useState<number | null>(null);
    const [recentlySavedCode, setRecentlySavedCode] = useState<string>("");
    const [sorting, setSorting] = useState<MRT_SortingState>([]);
    const [tileMode, setTileMode] = useState<"date" | "beginning">("date");
    const [dateModeRange, setDateModeRange] = useState(() => {
        const defaults = getDefaultContactFilters();
        return { fromDate: defaults.fromDate, toDate: defaults.toDate };
    });
    const [nameSuggestions, setNameSuggestions] = useState<string[]>([]);
    const [counts, setCounts] = useState({
        total: 0,
        active: 0,
        inactive: 0,
    });
    const [auditNames, setAuditNames] = useState<Record<number, string>>({});
    const [tableConfig, setTableConfig] = useState<ContactTableColumnConfig[]>([]);
    const [formOpen, setFormOpen] = useState(false);
    const [editingContact, setEditingContact] = useState<ContactDetail | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<ContactListItem | null>(null);
    const [deleting, setDeleting] = useState(false);
    const [uploadOpen, setUploadOpen] = useState(false);
    const [previewOpen, setPreviewOpen] = useState(false);
    const [excelFile, setExcelFile] = useState<File | null>(null);
    const [excelNew, setExcelNew] = useState<ExcelPreviewRow[]>([]);
    const [excelUpdates, setExcelUpdates] = useState<ExcelPreviewRow[]>([]);
    const [excelDuplicates, setExcelDuplicates] = useState<ExcelPreviewRow[]>([]);
    const [excelInvalid, setExcelInvalid] = useState<ExcelPreviewRow[]>([]);
    const [excelPreviewMessage, setExcelPreviewMessage] = useState("");
    const [validatingExcel, setValidatingExcel] = useState(false);
    const [savingExcel, setSavingExcel] = useState(false);
    const [downloadOpen, setDownloadOpen] = useState(false);
    const [downloadingExcel, setDownloadingExcel] = useState(false);
    const [historyContact, setHistoryContact] = useState<{
        id: number;
        contactCode: string;
    } | null>(null);
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState<"success" | "error" | "warning">("success");
    const lastContactRequestRef = useRef("");
    const initialBoardLoadRef = useRef(false);
    const requestedUserNameIdsRef = useRef<Set<number>>(new Set());
    const showMessage = useCallback((text: string, type: "success" | "error" | "warning" = "success") => {
        setMessage(text);
        setMessageType(type);
    }, []);
    const loadContacts = useCallback(async (force = false) => {
        const defaults = getDefaultContactFilters();
        const request = {
            ...toApiFilters(defaults),
            search: undefined,
            page: 0,
            size: pagination.pageSize,
            sort: defaults.sortBy || "id",
            direction: defaults.sortDirection || "desc",
        };
        const requestKey = JSON.stringify({ mode: "date", request });
        if (!force && lastContactRequestRef.current === requestKey)
            return;
        lastContactRequestRef.current = requestKey;
        try {
            await dispatch(filterContacts(request)).unwrap();
            dispatch(setAppliedFiltersAction(defaults));
        }
        catch {
            if (lastContactRequestRef.current === requestKey)
                lastContactRequestRef.current = "";
        }
    }, [dispatch, pagination.pageSize]);
    const loadCounts = useCallback(async (mode: "date" | "beginning" = tileMode, filters: ContactFilters = draftFilters) => {
        try {
            const data = await getStatusCount(mode === "date"
                ? { fromDate: filters.fromDate || undefined, toDate: filters.toDate || undefined }
                : {});
            setCounts({ total: data.active + data.inactive, active: data.active, inactive: data.inactive });
        }
        catch {
            setCounts({ total: 0, active: 0, inactive: 0 });
        }
    }, [draftFilters, tileMode]);
    const loadDropdowns = useCallback(async (_force = false) => {
        try {
            const master = await getDropdowns();
            dispatch(setDropdownCache(master));
            setFilterDropdowns(master);
        }
        catch {
            if (!storedDropdowns)
                dispatch(setDropdownCache(EMPTY_DROPDOWNS));
            setFilterDropdowns(storedDropdowns ?? EMPTY_DROPDOWNS);
        }
    }, [dispatch, storedDropdowns]);
    const refreshContactBoard = useCallback(async () => {
        const request = appliedFilters.search.trim()
            ? { search: appliedFilters.search.trim() }
            : tileMode === "beginning"
                ? { status: appliedFilters.status === "Active" ? false : appliedFilters.status === "Inactive" ? true : undefined }
                : toApiFilters(appliedFilters);
        lastContactRequestRef.current = "";
        setPagination((old) => ({ ...old, pageIndex: 0 }));
        await dispatch(filterContacts({
            ...request,
            page: 0,
            size: pagination.pageSize,
            sort: appliedFilters.sortBy || "id",
            direction: appliedFilters.sortDirection || "desc",
        })).unwrap();
        await Promise.all([loadCounts(tileMode, appliedFilters), loadDropdowns(true)]);
    }, [appliedFilters, dispatch, loadCounts, loadDropdowns, pagination.pageSize, tileMode]);

    const loadPage = useCallback(async (nextPagination: MRT_PaginationState) => {
        const request = appliedFilters.search.trim()
            ? { search: appliedFilters.search.trim() }
            : tileMode === "beginning"
                ? { status: appliedFilters.status === "Active" ? false : appliedFilters.status === "Inactive" ? true : undefined }
                : toApiFilters(appliedFilters);
        await dispatch(filterContacts({
            ...request,
            page: nextPagination.pageIndex,
            size: nextPagination.pageSize,
            sort: appliedFilters.sortBy || "id",
            direction: appliedFilters.sortDirection || "desc",
        })).unwrap();
    }, [appliedFilters, dispatch, tileMode]);

    const handlePaginationChange = useCallback((next: MRT_PaginationState) => {
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        setPagination(next);
        void loadPage(next);
    }, [loadPage]);

    const loadTableConfig = useCallback(async () => {
        try {
            setTableConfig(await getContactTableConfig());
        }
        catch {
            setTableConfig([]);
        }
    }, []);
    useEffect(() => {
        if (initialBoardLoadRef.current)
            return;
        initialBoardLoadRef.current = true;
        void Promise.allSettled([
            loadContacts(),
            loadTableConfig(),
            loadCounts(),
            loadDropdowns(true),
        ]);
    }, [loadContacts, loadTableConfig, loadCounts, loadDropdowns]);
    useEffect(() => {
        if (contactError) {
            showMessage(contactError, "error");
        }
    }, [contactError, showMessage]);
    useEffect(() => {
        const known = contacts.reduce<Record<number, string>>((result, contact) => {
            if (typeof contact.createdBy === "number" && contact.createdBy > 0 && contact.createdByName)
                result[contact.createdBy] = contact.createdByName;
            if (typeof contact.updatedBy === "number" && contact.updatedBy > 0 && contact.updatedByName)
                result[contact.updatedBy] = contact.updatedByName;
            return result;
        }, {});
        setAuditNames((current) => {
            const changed = Object.entries(known).some(([id, name]) => current[Number(id)] !== name);
            return changed ? { ...current, ...known } : current;
        });
        const ids = Array.from(new Set(contacts
            .flatMap((contact) => [contact.createdBy, contact.updatedBy])
            .filter((id): id is number => typeof id === "number" && id > 0)));
        const missing=ids.filter((id)=>!known[id]&&!auditNames[id]&&!requestedUserNameIdsRef.current.has(id));
        if(!missing.length)return;
        missing.forEach((id)=>requestedUserNameIdsRef.current.add(id));
        let cancelled=false;
        void getUserNames(missing).then((names)=>{
            if(cancelled)return;
            setAuditNames((current)=>{
                const next={...current};
                let changed=false;
                Object.entries(names).forEach(([id,name])=>{
                    const key=Number(id);
                    const value=String(name??"").trim();
                    if(Number.isFinite(key)&&value&&next[key]!==value){
                        next[key]=value;
                        changed=true;
                    }
                });
                return changed?next:current;
            });
        }).catch(()=>{
            missing.forEach((id)=>requestedUserNameIdsRef.current.delete(id));
        });
        return()=>{cancelled=true;};
    }, [contacts]);
    const selectedTile = useMemo(() => {
        if (draftFilters.status === "Active")
            return "active" as const;
        if (draftFilters.status === "Inactive") {
            return "inactive" as const;
        }
        return "all" as const;
    }, [draftFilters.status]);
    const handleNameQueryChange = (query: string) => {
        const value = query.trim().toLowerCase();
        if (!value) {
            setNameSuggestions([]);
            return;
        }
        const suggestions = Array.from(new Set([
            ...masterDropdowns.names,
            ...contacts.map((contact) => String(contact.name ?? "").trim()),
        ].filter(Boolean)))
            .filter((name) => name.toLowerCase().startsWith(value))
            .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: "base" }))
            .slice(0, 10);
        setNameSuggestions(suggestions);
    };
    // Draft-only filter changes. Typing/selecting never calls the contact list API.
    const handleFilterChange = <K extends keyof ContactFilters>(field: K, value: ContactFilters[K]) => {
        const nextFilters = { ...draftFilters, [field]: value };
        if (field === "fromDate") {
            const fromDate = String(value);
            const date = new Date(`${fromDate}T12:00:00`);
            date.setDate(date.getDate() + 6);
            const lastAllowed = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
            const today = new Date();
            const currentDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
            if (!nextFilters.toDate || nextFilters.toDate < fromDate) nextFilters.toDate = fromDate;
            if (nextFilters.toDate > lastAllowed) nextFilters.toDate = lastAllowed;
            if (nextFilters.toDate > currentDate) nextFilters.toDate = currentDate;
        }
        if (field === "search") setNameSuggestions([]);
        if (tileMode === "date" && (field === "fromDate" || field === "toDate"))
            setDateModeRange({ fromDate: nextFilters.fromDate, toDate: nextFilters.toDate });
        dispatch(setDraftFiltersAction(nextFilters));
    };
    // Advanced Search: API runs only when the effective table filters actually changed.
    const handleSearch = async (filters: ContactFilters) => {
        const nextFilters: ContactFilters = { ...filters, search: "" };
        const currentFilters: ContactFilters = { ...appliedFilters, search: "" };
        const unchanged = tileMode === "date" &&
            nextFilters.name === currentFilters.name &&
            nextFilters.contactType === currentFilters.contactType &&
            nextFilters.department === currentFilters.department &&
            nextFilters.city === currentFilters.city &&
            nextFilters.status === currentFilters.status &&
            nextFilters.fromDate === currentFilters.fromDate &&
            nextFilters.toDate === currentFilters.toDate &&
            nextFilters.sortBy === currentFilters.sortBy &&
            nextFilters.sortDirection === currentFilters.sortDirection;
        if (unchanged) return;
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        setDateModeRange({ fromDate: nextFilters.fromDate, toDate: nextFilters.toDate });
        dispatch(setAppliedFiltersAction(nextFilters));
        setTileMode("date");
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        await Promise.all([
            dispatch(filterContacts({
                ...toApiFilters(nextFilters), search: undefined, page: 0, size: pagination.pageSize,
                sort: nextFilters.sortBy || "id", direction: nextFilters.sortDirection || "desc",
            })).unwrap(),
            loadCounts("date", nextFilters),
        ]);
    };
    // Wild Search: icon only, full database, advanced/date filters are ignored.
    const handleWildSearch = async (search: string) => {
        const cleanSearch = search.trim();
        if (!cleanSearch)
            return;
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        const nextApplied = { ...getDefaultContactFilters(), search: cleanSearch, status: "", fromDate: "", toDate: "" };
        dispatch(setAppliedFiltersAction(nextApplied));
        setTileMode("beginning");
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        await Promise.all([
            dispatch(filterContacts({
                search: cleanSearch,
                page: 0,
                size: pagination.pageSize,
                sort: draftFilters.sortBy || "id",
                direction: draftFilters.sortDirection || "desc",
            })).unwrap(),
            loadCounts("beginning", draftFilters),
        ]);
    };
    const handleReset = async (_filters: ContactFilters) => {
        const nextFilters: ContactFilters = {
            ...getDefaultContactFilters(), status: "Active", search: "", name: "", contactType: "", department: "", city: "",
            fromDate: dateModeRange.fromDate, toDate: dateModeRange.toDate, sortBy: "", sortDirection: "",
        };
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        setNameSuggestions([]);
        setTileMode("date");
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        setTableResetKey((value) => value + 1);
        dispatch(setDraftFiltersAction(nextFilters));
        dispatch(setAppliedFiltersAction(nextFilters));
        await Promise.all([
            dispatch(filterContacts({
                ...toApiFilters(nextFilters), search: undefined, page: 0, size: pagination.pageSize,
                sort: "id", direction: "desc",
            })).unwrap(),
            loadCounts("date", nextFilters),
        ]);
    };
    const handleModeChange = async (mode: "date" | "beginning") => {
        if (mode === tileMode) return;
        if (mode === "beginning") {
            setTileMode("beginning");
            await loadCounts("beginning", draftFilters);
            return;
        }
        const nextFilters: ContactFilters = {
            ...draftFilters, search: "",
            fromDate: dateModeRange.fromDate, toDate: dateModeRange.toDate,
        };
        setTileMode("date");
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        dispatch(setDraftFiltersAction(nextFilters));
        dispatch(setAppliedFiltersAction(nextFilters));
        await Promise.all([
            dispatch(filterContacts({
                ...toApiFilters(nextFilters), search: undefined, page: 0, size: pagination.pageSize,
                sort: nextFilters.sortBy || "id", direction: nextFilters.sortDirection || "desc",
            })).unwrap(),
            loadCounts("date", nextFilters),
        ]);
    };
    const handleTileChange = async (value: "all" | "active" | "inactive") => {
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        const status: ContactFilters["status"] = value === "active" ? "Active" : value === "inactive" ? "Inactive" : "";
        const nextApplied: ContactFilters = {
            ...getDefaultContactFilters(), status, search: "", name: "", contactType: "", department: "", city: "",
            fromDate: tileMode === "date" ? draftFilters.fromDate : "",
            toDate: tileMode === "date" ? draftFilters.toDate : "",
            sortBy: draftFilters.sortBy, sortDirection: draftFilters.sortDirection,
        };
        setPagination((page) => ({ ...page, pageIndex: 0 }));
        await dispatch(filterContacts({
            status: status === "Active" ? false : status === "Inactive" ? true : undefined,
            fromDate: tileMode === "date" ? draftFilters.fromDate || undefined : undefined,
            toDate: tileMode === "date" ? draftFilters.toDate || undefined : undefined,
            page: 0, size: pagination.pageSize, sort: draftFilters.sortBy || "id", direction: draftFilters.sortDirection || "desc",
        })).unwrap();
        if (tileMode === "beginning") {
            dispatch(setDraftFiltersAction({ ...draftFilters, status, fromDate: "", toDate: "" }));
            dispatch(setAppliedFiltersAction(nextApplied));
        }
        else {
            setDateModeRange({ fromDate: draftFilters.fromDate, toDate: draftFilters.toDate });
            dispatch(setDraftFiltersAction({ ...draftFilters, status }));
            dispatch(setAppliedFiltersAction(nextApplied));
        }
    };
    const handleRemoveFilter = async (key: keyof ContactFilters) => {
        if (key === "fromDate" || key === "toDate")
            return;
        const draftValue = draftFilters[key];
        const appliedValue = appliedFilters[key];
        // Chip belongs only to an un-applied More Filter change:
        // remove/revert the draft chip only; table + API stay untouched.
        if (draftValue !== appliedValue) {
            const nextDraft: ContactFilters = { ...draftFilters, [key]: appliedValue } as ContactFilters;
            if (key === "sortBy" && !appliedValue) {
                nextDraft.sortDirection = appliedFilters.sortDirection;
            }
            dispatch(setDraftFiltersAction(nextDraft));
            if (key === "search")
                setNameSuggestions([]);
            return;
        }
        // Nothing is currently applied for this chip, so only clean the draft UI.
        if (!appliedValue) {
            const nextDraft: ContactFilters = { ...draftFilters, [key]: "" } as ContactFilters;
            if (key === "sortBy")
                nextDraft.sortDirection = "";
            dispatch(setDraftFiltersAction(nextDraft));
            if (key === "search")
                setNameSuggestions([]);
            return;
        }
        // This chip is part of the currently applied table request.
        // Removing it must run the API and reload the grid from page 1.
        const nextApplied: ContactFilters = { ...appliedFilters, [key]: "" } as ContactFilters;
        const nextDraft: ContactFilters = { ...draftFilters, [key]: "" } as ContactFilters;
        if (key === "sortBy") {
            nextApplied.sortDirection = "";
            nextDraft.sortDirection = "";
        }
        setRecentlyUpdatedId(null);
        setRecentlySavedCode("");
        dispatch(setAppliedFiltersAction(nextApplied));
        dispatch(setDraftFiltersAction(nextDraft));
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        setTableResetKey((value) => value + 1);
        if (key === "search")
            setNameSuggestions([]);
        const request = nextApplied.search.trim()
            ? { search: nextApplied.search.trim() }
            : tileMode === "beginning"
                ? {
                    status: nextApplied.status === "Active"
                        ? false
                        : nextApplied.status === "Inactive"
                            ? true
                            : undefined,
                }
                : toApiFilters(nextApplied);
        await dispatch(filterContacts({
            ...request,
            page: 0,
            size: pagination.pageSize,
            sort: nextApplied.sortBy || "id",
            direction: nextApplied.sortDirection || "desc",
        })).unwrap();
    };
    const downloadAttachment = useCallback(async (uuid?: string) => {
        if (!uuid)
            return;
        try {
            const file = await downloadContactFile(uuid);
            const blobUrl = URL.createObjectURL(file.blob);
            const link = document.createElement("a");
            link.href = blobUrl;
            link.download = file.fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
        }
        catch (error) {
            showMessage(error instanceof Error
                ? error.message
                : "Unable to download attachment", "error");
        }
    }, [showMessage]);
    const contactColumns = useMemo<MRT_ColumnDef<ContactListItem>[]>(() => {
        const baseColumns: MRT_ColumnDef<ContactListItem>[] = [
            {
                id: "action",
                header: "Action",
                enableSorting: false,
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => (<Box sx={{ display: "flex" }}>
              {canUpdate && (<>
                  <IconButton size="small" onClick={(event) => {
                            event.currentTarget.blur();
                            blurActiveElement();
                            openEdit(row.original);
                        }}><EditOutlinedIcon /></IconButton>
                  {isAdmin && !row.original.status && (<IconButton size="small" color="error" onClick={(event) => {
                                event.currentTarget.blur();
                                blurActiveElement();
                                setDeleteTarget(row.original);
                            }}><DeleteOutlineOutlinedIcon /></IconButton>)}
                </>)}
            </Box>),
            },
            {
                id: "contactCode",
                header: "Contact Code",
                accessorFn: (row: ContactListItem) => displayValue(row.contactCode),
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => (<Button size="small" variant="text" onClick={() => setHistoryContact({
                        id: row.original.id,
                        contactCode: row.original.contactCode,
                    })} title="View contact history" sx={{ color: "#1976d2", fontWeight: 700, textTransform: "none", minWidth: 0, p: 0, textDecoration: "underline", textUnderlineOffset: "2px", cursor: "pointer", "&:hover": { bgcolor: "transparent", color: "#0d47a1", textDecoration: "underline" } }}>
              {displayValue(row.original.contactCode)}
            </Button>),
            },
            {
                id: "name",
                header: "Name",
                accessorFn: (row: ContactListItem) => displayValue(row.name),
            },
            {
                id: "contactType",
                header: "Contact Type",
                accessorFn: (row: ContactListItem) => displayValue(row.contactType),
            },
            {
                id: "mobile",
                header: "Mobile Number",
                accessorFn: (row: ContactListItem) => displayValue(row.mobile),
            },
            {
                id: "email",
                header: "Email ID",
                accessorFn: (row: ContactListItem) => displayValue(row.email),
            },
            {
                id: "department",
                header: "Department",
                accessorFn: (row: ContactListItem) => displayValue(row.department),
            },
            {
                id: "designation",
                header: "Designation",
                accessorFn: (row: ContactListItem) => displayValue(row.designation),
            },
            {
                id: "companyName",
                header: "Company Name",
                accessorFn: (row: ContactListItem) => displayValue(row.companyName),
            },
            {
                id: "city",
                header: "City",
                accessorFn: (row: ContactListItem) => displayValue(row.city),
            },
            {
                id: "photo",
                header: "Photo",
                enableSorting: false,
                size: 72,
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => row.original.photoUuid ? (<PhotoDownloadCell uuid={row.original.photoUuid} fileName={row.original.photoFile?.fileName} onDownload={downloadAttachment}/>) : ("N/A"),
            },
            {
                id: "documents",
                header: "Documents",
                enableSorting: false,
                size: 90,
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => (<DocumentsDownloadCell uuids={row.original.documentUuids || []} files={row.original.documentFiles} onDownload={downloadAttachment}/>),
            },
            {
                id: "status",
                header: "Status",
                accessorFn: (row: ContactListItem) => row.status ? "Inactive" : "Active",
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => (<Chip size="small" label={row.original.status
                        ? "Inactive"
                        : "Active"} color={row.original.status
                        ? "error"
                        : "success"}/>),
            },
            {
                id: "createdBy",
                header: "Created By",
                enableSorting: false,
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => {
                    const id = row.original.createdBy;
                    return row.original.createdByName || (typeof id === "number" && id > 0 ? auditNames[id] || "N/A" : "N/A");
                },
            },
            {
                accessorKey: "createdAt",
                header: "Created At",
                Cell: ({ cell }) => formatDateTime(cell.getValue<string | null>()),
            },
            {
                id: "updatedBy",
                header: "Updated By",
                enableSorting: false,
                Cell: ({ row }: {
                    row: MRT_Row<ContactListItem>;
                }) => {
                    const id = row.original.updatedBy;
                    if (!row.original.updatedAt ||
                        typeof id !== "number" ||
                        id <= 0) {
                        return "N/A";
                    }
                    return row.original.updatedByName || auditNames[id] || "N/A";
                },
            },
            {
                accessorKey: "updatedAt",
                header: "Updated At",
                Cell: ({ cell }) => formatDateTime(cell.getValue<string | null>()),
            },
        ];
        if (!canUpdate)
            baseColumns.shift();
        if (!tableConfig.length)
            return baseColumns;
        const normalize = (value: string) => value.replace(/[^a-z0-9]/gi, "").toLowerCase();
        const configByKey = new Map(tableConfig.map((item) => [
            normalize(item.key),
            item,
        ]));
        const configured = baseColumns
            .map((column, index) => {
            const key = String((column as any).id ??
                (column as any).accessorKey ??
                "");
            const config = configByKey.get(normalize(key));
            if (!config) {
                return {
                    column,
                    index,
                    order: Number.MAX_SAFE_INTEGER,
                };
            }
            if (config.visible === false)
                return null;
            return {
                column: {
                    ...column,
                    header: config.header ||
                        String((column as any).header || key),
                },
                index,
                order: Number.isFinite(config.order)
                    ? Number(config.order)
                    : index,
            };
        })
            .filter(Boolean) as {
            column: MRT_ColumnDef<ContactListItem>;
            index: number;
            order: number;
        }[];
        const fixedOrder: Record<string, number> = {
            action: 1,
            contactCode: 2,
            name: 3,
        };
        return configured
            .sort((a, b) => {
            const aKey = String((a.column as any).id ?? (a.column as any).accessorKey ?? "");
            const bKey = String((b.column as any).id ?? (b.column as any).accessorKey ?? "");
            const aPriority = fixedOrder[aKey];
            const bPriority = fixedOrder[bKey];
            if (aPriority && bPriority)
                return aPriority - bPriority;
            if (aPriority)
                return -1;
            if (bPriority)
                return 1;
            return a.order - b.order || a.index - b.index;
        })
            .map((item) => item.column);
    }, [
        auditNames,
        canUpdate,
        downloadAttachment,
        isAdmin,
        tableConfig,
    ]);
    const previewColumns = useMemo<MRT_ColumnDef<ExcelPreviewRow>[]>(() => [
        {
            id: "contactType",
            header: "Contact Type",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.contactType),
        },
        {
            id: "name",
            header: "Name",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.name),
        },
        {
            accessorKey: "mobile",
            header: "Mobile",
        },
        {
            accessorKey: "email",
            header: "Email",
        },
        {
            id: "department",
            header: "Department",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.department),
        },
        {
            id: "designation",
            header: "Designation",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.designation),
        },
        {
            id: "companyName",
            header: "Company Name",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.companyName),
        },
        {
            id: "city",
            header: "City",
            accessorFn: (row: ExcelPreviewRow) => displayValue(row.city),
        },
        {
            accessorKey: "previousStatus",
            header: "Previous Status",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => row.original.previousStatus ? (<Chip size="small" label={row.original.previousStatus} color={row.original.previousStatus ===
                    "Inactive"
                    ? "error"
                    : "success"}/>) : ("N/A"),
        },
        {
            accessorKey: "status",
            header: "New Status",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => row.original.status ? (<Chip size="small" label={row.original.status} color={row.original.status === "Inactive"
                    ? "error"
                    : "success"}/>) : ("N/A"),
        },
        {
            accessorKey: "action",
            header: "Action",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => row.original.action === "Update" ? (<Chip size="small" color="info" label="Update"/>) : row.original.action === "New" ? (<Chip size="small" color="success" label="New Save"/>) : ("N/A"),
        },
        {
            accessorKey: "updateType",
            header: "Update Type",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => row.original.updateType || "N/A",
        },
        {
            accessorKey: "changedFields",
            header: "Changed Fields",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => row.original.changedFields || "N/A",
        },
        {
            accessorKey: "error",
            header: "Result",
            Cell: ({ row }: {
                row: MRT_Row<ExcelPreviewRow>;
            }) => {
                const result = row.original.error;
                if (!result) {
                    return (<Chip size="small" color="success" label="Correct"/>);
                }
                if (result === "Saved") {
                    return (<Chip size="small" color="success" label="Saved"/>);
                }
                const duplicate = result
                    .toLowerCase()
                    .includes("duplicate") ||
                    result
                        .toLowerCase()
                        .includes("already exist");
                return (<Chip size="small" color={duplicate ? "warning" : "error"} label={result}/>);
            },
        },
    ], []);
    const openEdit = (contact: ContactListItem) => {
        blurActiveElement();
        setEditingContact(contact);
        setFormOpen(true);
    };
    const handleAdd = async (payload: ContactRequestDto[]) => {
        const response = await saveContact(payload);
        if (response.data.successCount > 0) {
            setRecentlyUpdatedId(null);
            setRecentlySavedCode(response.data.saved[0]?.contactCode || "");
            await refreshContactBoard();
            setPagination((value) => ({ ...value, pageIndex: 0 }));
            setTableResetKey((value) => value + 1);
        }
        showMessage(response.message || "Contact saved successfully", response.data.duplicateCount || response.data.invalidCount
            ? "warning"
            : "success");
        return response;
    };
    const handleUpdate = async (payload: ContactRequestDto): Promise<void> => {
        const oldPhotoUuid = editingContact?.photoUuid || "";
        const oldDocumentUuids = editingContact?.documentUuids || [];
        const nextDocumentUuids = new Set(payload.documentUuids || []);
        const removedUuids = [
            ...(oldPhotoUuid && oldPhotoUuid !== payload.photoUuid ? [oldPhotoUuid] : []),
            ...oldDocumentUuids.filter((uuid) => uuid && !nextDocumentUuids.has(uuid)),
        ];
        const response = await saveContact(payload);
        const cleanupResults = await Promise.allSettled([...new Set(removedUuids)].map((uuid) => deleteContactFile(uuid)));
        const cleanupFailed = cleanupResults.some((result) => result.status === "rejected");
        setFormOpen(false);
        setEditingContact(null);
        await refreshContactBoard();
        setRecentlySavedCode("");
        setRecentlyUpdatedId(payload.id ?? null);
        setPagination((value) => ({ ...value, pageIndex: 0 }));
        setTableResetKey((value) => value + 1);
        showMessage(cleanupFailed
            ? "Contact updated, but one or more removed attachments could not be deleted from storage"
            : response.message || "Contact updated successfully", cleanupFailed ? "warning" : "success");
    };
    const confirmDelete = async () => {
        if (!deleteTarget)
            return;
        setDeleting(true);
        try {
            await deactivateContact(deleteTarget.id);
            setDeleteTarget(null);
            await refreshContactBoard();
            showMessage("Contact marked inactive successfully", "success");
        }
        catch (error) {
            showMessage(error instanceof Error
                ? error.message
                : "Unable to delete contact", "error");
        }
        finally {
            setDeleting(false);
        }
    };
    const closeExcel = (force = false) => {
        if (!force && validatingExcel)
            return;
        setUploadOpen(false);
        setPreviewOpen(false);
        setExcelFile(null);
        setExcelNew([]);
        setExcelUpdates([]);
        setExcelDuplicates([]);
        setExcelInvalid([]);
        setExcelPreviewMessage("");
    };
    const uploadExcel = async () => {
        if (!excelFile)
            return;
        setValidatingExcel(true);
        try {
            const response = await validateContactsExcel(excelFile);
            setExcelNew(response.data.newRecords);
            setExcelUpdates(response.data.updateRecords);
            setExcelDuplicates(response.data.duplicates);
            setExcelInvalid(response.data.invalid);
            setExcelPreviewMessage(response.message);
            setUploadOpen(false);
            blurActiveElement();
            setPreviewOpen(true);
        }
        catch (error) {
            const message = error instanceof Error
                ? error.message
                : "Invalid Excel file";
            setExcelNew([]);
            setExcelUpdates([]);
            setExcelDuplicates([]);
            setExcelInvalid([]);
            setExcelPreviewMessage(message.startsWith("Invalid Excel file")
                ? message
                : `Invalid Excel file. ${message}`);
            setUploadOpen(false);
            blurActiveElement();
            setPreviewOpen(true);
        }
        finally {
            setValidatingExcel(false);
        }
    };
    const resetExcel = () => {
        blurActiveElement();
        setPreviewOpen(false);
        setExcelFile(null);
        setExcelNew([]);
        setExcelUpdates([]);
        setExcelDuplicates([]);
        setExcelInvalid([]);
        setExcelPreviewMessage("");
        setUploadOpen(true);
    };
    const saveExcel = async () => {
        if (!excelFile ||
            (!excelNew.length && !excelUpdates.length)) {
            return;
        }
        setSavingExcel(true);
        try {
            const response = await importContactsExcel(excelFile);
            closeExcel(true);
            await refreshContactBoard();
            showMessage(response.message, response.data.duplicateCount ||
                response.data.invalidCount
                ? "warning"
                : "success");
        }
        catch (error) {
            showMessage(error instanceof Error
                ? error.message
                : "Unable to save Excel contacts", "error");
        }
        finally {
            setSavingExcel(false);
        }
    };
    const downloadFormat = async () => {
        try {
            const blob = await downloadContactFormat();
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = "Format.xlsx";
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        }
        catch (error) {
            showMessage(error instanceof Error
                ? error.message
                : "Unable to download format", "error");
        }
    };
    const openDownload = () => {
        blurActiveElement();
        setDownloadOpen(true);
    };
    const downloadExcel = async () => {
        const defaultSevenDays = isDefaultOneWeekRange(draftFilters);
        setDownloadingExcel(true);
        try {
            if (!defaultSevenDays) {
                const request = await requestContactsExcelDownload(toApiFilters(draftFilters));
                showMessage(request.message ||
                    (isAdmin
                        ? "Excel sent directly to your login email with attachment"
                        : "Download request sent to Admin for approval"), "success");
                setDownloadOpen(false);
                return;
            }
            const blob = await downloadContactsExcel(toApiFilters(draftFilters));
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = "contacts.xlsx";
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
            setDownloadOpen(false);
        }
        catch (error) {
            showMessage(error instanceof Error ? error.message : "Unable to download Excel", "error");
        }
        finally {
            setDownloadingExcel(false);
        }
    };
    const visibleContacts = useMemo(() => {
        if (recentlySavedCode) {
            const saved = contacts.find((contact) => contact.contactCode === recentlySavedCode);
            if (saved)
                return [saved, ...contacts.filter((contact) => contact.contactCode !== recentlySavedCode)];
        }
        if (recentlyUpdatedId) {
            const updated = contacts.find((contact) => contact.id === recentlyUpdatedId);
            if (updated)
                return [updated, ...contacts.filter((contact) => contact.id !== recentlyUpdatedId)];
        }
        return contacts;
    }, [contacts, recentlySavedCode, recentlyUpdatedId]);
    const displayedResultCount = totalElements;
    const labels = useMemo(() => getAppliedFilterLabels(draftFilters), [draftFilters]);
    const dialogTitleSx = {
        bgcolor: "#0f9187",
        color: "#fff",
        fontWeight: 700,
    };
    const primaryButtonSx = {
        minWidth: 110,
        height: 38,
        textTransform: "none",
        fontWeight: 700,
        bgcolor: "#0f9187",
        "&:hover": {
            bgcolor: "#087a72",
        },
    };
    const resetButtonSx = {
        minWidth: 110,
        height: 38,
        textTransform: "none",
        fontWeight: 700,
        borderColor: "#64748b",
        color: "#475569",
        "&:hover": {
            borderColor: "#475569",
            bgcolor: "#f8fafc",
        },
    };
    const cancelButtonSx = {
        minWidth: 110,
        height: 38,
        textTransform: "none",
        fontWeight: 700,
        borderColor: "#dc2626",
        color: "#dc2626",
        "&:hover": {
            borderColor: "#b91c1c",
            bgcolor: "#fef2f2",
        },
    };
    const fabSx = (bottom: number) => ({
        position: "fixed" as const,
        right: 32,
        bottom,
        bgcolor: "#0f9187",
        color: "#fff",
        "&:hover": {
            bgcolor: "#087a72",
            color: "#fff",
        },
        "&:focus": {
            bgcolor: "#0f9187",
            color: "#fff",
        },
        "&:active": {
            bgcolor: "#087a72",
            color: "#fff",
        },
        "&.Mui-focusVisible": {
            bgcolor: "#087a72",
            color: "#fff",
        },
    });
    return (<Box sx={{ width: "100%", pb: 3 }}>
      <Paper elevation={0} sx={{px:2,py:1.5,mb:1.25,bgcolor:"#0f9187",color:"#fff"}}>
        <Box sx={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <Typography sx={{fontSize:20,fontWeight:700}}>Contact Activity Board</Typography>
          <Tooltip title="Hard Reload">
            <IconButton size="small" aria-label="Hard Reload" onClick={()=>window.location.reload()} sx={{color:"#fff","&:hover":{bgcolor:"rgba(255,255,255,0.14)"}}}>
              <RefreshOutlinedIcon fontSize="small"/>
            </IconButton>
          </Tooltip>
        </Box>
      </Paper>
      <ContactActivityFilter value={draftFilters} dropdowns={filterDropdowns} searching={false} total={counts.total} active={counts.active} inactive={counts.inactive} selectedTile={selectedTile} mode={tileMode} onModeChange={handleModeChange} onChange={handleFilterChange} onWildSearch={handleWildSearch} onSearch={handleSearch} onReset={handleReset} onTileChange={handleTileChange} nameSuggestions={nameSuggestions} onNameQueryChange={handleNameQueryChange}/>
      <Paper elevation={0} sx={{ px: 1.25, py: 1, mb: 1, border: "1px solid #d6e4e1", borderRadius: 1.5, }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, flexWrap: "wrap", }}>
          <Typography sx={{ fontSize: 12, fontWeight: 700, color: "#475569", mr: 0.25, }}>
            Filter By:
          </Typography>
          {labels.length > 0 ? (labels.map((item) => (<Chip key={item.key} size="small" label={item.label} onDelete={item.key === "fromDate" ||
                item.key === "toDate"
                ? undefined
                : () => handleRemoveFilter(item.key)} sx={{
                bgcolor: "#eef8f7",
                color: "#315c58",
                border: "1px solid #b9ddd9",
                ...(item.key !== "fromDate" &&
                    item.key !== "toDate"
                    ? {
                        "& .MuiChip-deleteIcon": {
                            color: "#d32f2f",
                            fontSize: 18,
                            opacity: 0,
                            width: 0,
                            margin: 0,
                            transition: "opacity .15s ease,width .15s ease,margin .15s ease",
                            "&:hover": {
                                color: "#b71c1c",
                            },
                        },
                        "&:hover .MuiChip-deleteIcon": {
                            opacity: 1,
                            width: 18,
                            margin: "0 5px 0 -3px",
                        },
                    }
                    : {}),
            }}/>))) : (<Typography sx={{ fontSize: 12, color: "#64748b", }}>
              No filter applied
            </Typography>)}
          <Box sx={{ ml: "auto", display: "flex", alignItems: "center", gap: 0.5, }}>
            <Typography sx={{ fontSize: 12, fontWeight: 700, }}>
              {displayedResultCount} Results
            </Typography>
        {canDownloadGrid && (<IconButton size="small" aria-label="Download Excel" title="Download Excel" onClick={(event) => {
                event.currentTarget.blur();
                blurActiveElement();
                void openDownload();
            }} sx={{ color: "#0f9187" }}>
              <DownloadOutlinedIcon fontSize="small"/>
            </IconButton>)}
          </Box>
        </Box>
      </Paper>
      <Paper elevation={0} sx={{ p: 1, mb: 1, border: "1px solid #d6e4e1", borderRadius: 1.5, overflow: "hidden", }}>
        <ReactTable columns={contactColumns} data={visibleContacts} rowCount={totalElements} loading={loading} pagination={pagination} onPaginationChange={handlePaginationChange} manualPagination defaultPageSize={10} key={`${tableResetKey}|${tileMode}|${appliedFilters.search}|${appliedFilters.name}|${appliedFilters.contactType}|${appliedFilters.department}|${appliedFilters.city}|${appliedFilters.status}|${appliedFilters.fromDate}|${appliedFilters.toDate}|${appliedFilters.sortBy}|${appliedFilters.sortDirection}`} sorting={sorting} onSortingChange={(next) => {
            setSorting((current) => JSON.stringify(current) === JSON.stringify(next)
                ? current
                : next);
        }}/>
      </Paper>
      <Fab aria-label="Download Format" title="Download Format" onClick={(event) => {
            event.currentTarget.blur();
            blurActiveElement();
            void downloadFormat();
        }} sx={fabSx(72)}>
        <DownloadOutlinedIcon />
      </Fab>
      {isAdmin && (<Fab aria-label="Upload Excel" onClick={(event) => {
                event.currentTarget.blur();
                blurActiveElement();
                setExcelFile(null);
                setUploadOpen(true);
            }} sx={fabSx(136)}>
        <UploadOutlinedIcon />
      </Fab>)}
      {isAdmin && (<Fab aria-label="Add Contact" onClick={(event) => {
                event.currentTarget.blur();
                blurActiveElement();
                setEditingContact(null);
                setFormOpen(true);
            }} sx={fabSx(200)}>
        <AddOutlinedIcon />
      </Fab>)}
      <Dialog disableRestoreFocus open={uploadOpen} onClose={(_, reason) => {
            if (reason !== "backdropClick" &&
                reason !== "escapeKeyDown") {
                closeExcel();
            }
        }} fullWidth maxWidth="xs">
        <DialogTitle sx={dialogTitleSx}>
          Upload Excel
        </DialogTitle>
        <DialogContent dividers>
          <Typography sx={{ mb: 1.5, fontSize: 14, color: "#475569" }}>
            Select an Excel file to validate and import contact data.
          </Typography>
          <Button component="label" variant="outlined" fullWidth sx={{ textTransform: "none", fontWeight: 700 }}>
            {excelFile ? excelFile.name : "Choose Excel File"}
            <input hidden type="file" accept=".xlsx,.xls" onChange={(event) => setExcelFile(event.target.files?.[0] ?? null)}/>
          </Button>
        </DialogContent>
        <DialogActions sx={{ justifyContent: "center", gap: 1, p: 2, }}>
          <Button variant="outlined" sx={cancelButtonSx} onClick={() => closeExcel()} disabled={validatingExcel}>
            Cancel
          </Button>
          <Button variant="contained" sx={primaryButtonSx} onClick={uploadExcel} disabled={!excelFile ||
            validatingExcel}>
            {validatingExcel
            ? "Checking..."
            : "Upload"}
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog disableRestoreFocus open={previewOpen} onClose={(_, reason) => {
            if (reason !== "backdropClick" &&
                reason !== "escapeKeyDown") {
                closeExcel();
            }
        }} fullWidth maxWidth="lg">
        <DialogTitle sx={dialogTitleSx}>
          {excelPreviewMessage.startsWith("Invalid Excel file")
            ? "Invalid Excel File"
            : "Excel Data Review"}
        </DialogTitle>
        <DialogContent dividers>
          {excelPreviewMessage && (<Alert severity={excelPreviewMessage.startsWith("Invalid Excel file") || excelInvalid.length
                ? "error"
                : excelDuplicates.length
                    ? "warning"
                    : "success"} sx={{ mb: 1.5 }}>
              {excelPreviewMessage}
            </Alert>)}
          {excelNew.length > 0 && (<Box sx={{ mb: 2 }}>
              <Typography sx={{ mb: 0.75, fontSize: 14, fontWeight: 700, color: "#15803d" }}>
                New Data ({excelNew.length})
              </Typography>
              <ReactTable columns={previewColumns} data={excelNew} paginationEnabled maxHeight={240}/>
            </Box>)}

          {excelUpdates.length > 0 && (<Box sx={{ mb: 2 }}>
              <Typography sx={{ mb: 0.75, fontSize: 14, fontWeight: 700, color: "#0369a1" }}>
                Update Data ({excelUpdates.length})
              </Typography>
              <ReactTable columns={previewColumns} data={excelUpdates} paginationEnabled maxHeight={240}/>
            </Box>)}

          {excelDuplicates.length > 0 && (<Box sx={{ mb: 2 }}>
              <Typography sx={{ mb: 0.75, fontSize: 14, fontWeight: 700, color: "#a16207" }}>
                Duplicate Data ({excelDuplicates.length})
              </Typography>
              <ReactTable columns={previewColumns} data={excelDuplicates} paginationEnabled maxHeight={240}/>
            </Box>)}

          {excelInvalid.length > 0 && (<Box>
              <Typography sx={{ mb: 0.75, fontSize: 14, fontWeight: 700, color: "#b91c1c" }}>
                Incorrect Data ({excelInvalid.length})
              </Typography>
              <ReactTable columns={previewColumns} data={excelInvalid} paginationEnabled maxHeight={240}/>
            </Box>)}
        </DialogContent>
        <DialogActions sx={{ justifyContent: "center", gap: 1, p: 2, }}>
          {excelNew.length + excelUpdates.length > 0 ? (<>
              <Button variant="contained" sx={primaryButtonSx} onClick={saveExcel} disabled={savingExcel}>
                {savingExcel
                ? "Saving..."
                : "Save"}
              </Button>
              <Button variant="outlined" sx={resetButtonSx} onClick={resetExcel} disabled={savingExcel}>
                Reset
              </Button>
              <Button variant="outlined" sx={cancelButtonSx} onClick={() => closeExcel()} disabled={savingExcel}>
                Cancel
              </Button>
            </>) : (<Button variant="outlined" sx={cancelButtonSx} onClick={() => closeExcel()} disabled={savingExcel}>
              Cancel
            </Button>)}
        </DialogActions>
      </Dialog>
      <Dialog disableRestoreFocus open={downloadOpen} onClose={(_, reason) => {
            if (reason !== "backdropClick" &&
                reason !== "escapeKeyDown") {
                setDownloadOpen(false);
            }
        }} fullWidth maxWidth="xs">
        <DialogTitle sx={dialogTitleSx}>
          Download Excel
        </DialogTitle>
        <DialogContent dividers>
          {isDefaultOneWeekRange(appliedFilters) ? (<Alert severity="success">
              Current 7-day grid data will download directly without approval.
            </Alert>) : (<Alert severity="info">
              {isAdmin
                ? "This filtered date range is more than the default 7 days. Yes will generate the exact filtered Excel and send it directly to your login email as an attachment."
                : "This filtered date range is more than the default 7 days. Yes will create an approval request. After Admin approval, the exact filtered Excel will be sent to your login email as an attachment."}
            </Alert>)}
        </DialogContent>
        <DialogActions sx={{ justifyContent: "center", gap: 1, p: 2, }}>
          <Button variant="contained" sx={primaryButtonSx} startIcon={<DownloadOutlinedIcon />} onClick={downloadExcel} disabled={downloadingExcel}>
            {downloadingExcel
            ? (!isDefaultOneWeekRange(appliedFilters) ? "Processing..." : "Downloading...")
            : (!isDefaultOneWeekRange(appliedFilters) ? (isAdmin ? "Yes, Send Excel" : "Yes, Request") : "Download XL")}
          </Button>
          <Button variant="outlined" sx={cancelButtonSx} onClick={() => setDownloadOpen(false)}>
            {!isDefaultOneWeekRange(appliedFilters) ? "No" : "Cancel"}
          </Button>
        </DialogActions>
      </Dialog>
      <Drawer disableRestoreFocus anchor="right" open={formOpen} onClose={(_, reason) => {
            if (reason !== "backdropClick" &&
                reason !== "escapeKeyDown") {
                setFormOpen(false);
            }
        }} slotProps={{
            paper: {
                sx: {
                    width: {
                        xs: "100%",
                        sm: 720,
                        lg: 900,
                    },
                },
            },
        }}>
        <Box sx={{ display: "flex", alignItems: "center", p: 2, bgcolor: "#0f9187", color: "#fff", position: "sticky", top: 0, zIndex: 10, flexShrink: 0, }}>
          <Typography sx={{ flex: 1, fontWeight: 700, fontSize: 18, }}>
            {editingContact
            ? "Update Contact"
            : "Add Contact"}
          </Typography>
          <IconButton sx={{ color: "#dc2626", bgcolor: "#fff", "&:hover": { bgcolor: "#fef2f2", }, }} onClick={(event) => {
            event.currentTarget.blur();
            setFormOpen(false);
        }}>
            <CloseOutlinedIcon />
          </IconButton>
        </Box>
        <Box sx={{ p: 2 }}>
          {editingContact ? (<UpdateContactForm data={editingContact} dropdowns={masterDropdowns} onSave={handleUpdate} onClose={() => setFormOpen(false)}/>) : (<AddContactForm dropdowns={masterDropdowns} onSave={handleAdd} onClose={() => setFormOpen(false)}/>)}
        </Box>
      </Drawer>
      <Dialog disableRestoreFocus open={Boolean(deleteTarget)} onClose={() => !deleting &&
            setDeleteTarget(null)}>
        <DialogTitle sx={dialogTitleSx}>
          Delete Contact?
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete{" "}
            {deleteTarget?.name}?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>
            Cancel
          </Button>
          <Button color="error" variant="contained" onClick={confirmDelete} disabled={deleting}>
            {deleting
            ? "Deleting..."
            : "Delete"}
          </Button>
        </DialogActions>
      </Dialog>
      <ContactHistoryDialog open={Boolean(historyContact)} contactId={historyContact?.id ?? null} contactCode={historyContact?.contactCode ?? ""} onClose={() => setHistoryContact(null)}/>
      <Snackbar open={Boolean(message)} autoHideDuration={2500} onClose={() => setMessage("")}>
        <Alert severity={messageType} onClose={() => setMessage("")}>
          {message}
        </Alert>
      </Snackbar>
    </Box>);
}
export default ContactActivityBoard;
