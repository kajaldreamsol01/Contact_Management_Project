import { useMemo } from "react";
import {
  Alert,
  Box,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Tooltip,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import PhotoOutlinedIcon from "@mui/icons-material/PhotoOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import type { MRT_ColumnDef } from "material-react-table";
import ReactTable from "../ReactTable";
import {
  type ContactHistoryItem,
  type ContactTableColumnConfig,
} from "./apis";

type Props = {
  open: boolean;
  contactCode: string;
  items: ContactHistoryItem[];
  tableConfig: ContactTableColumnConfig[];
  loading?: boolean;
  error?: string;
  onClose: () => void;
};

const textValue = (value: unknown) => {
  if (Array.isArray(value)) return value.length ? value.join(", ") : "N/A";
  const text = String(value ?? "").trim();
  return text || "N/A";
};

const dateTime = (value: unknown) => {
  if (!value) return "N/A";
  const date = new Date(String(value));
  if (Number.isNaN(date.getTime())) return textValue(value);
  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
};

const actionLabel = (action: string) => {
  if (action === "INACTIVATED") return "Inactive";
  if (action === "REACTIVATED") return "Active";
  if (action === "CREATED") return "Created";
  if (action === "UPDATED") return "Updated";
  return action.replaceAll("_", " ");
};

const actionColor = (action: string) => {
  if (action === "CREATED" || action === "REACTIVATED") return "success" as const;
  if (action === "INACTIVATED") return "error" as const;
  return "info" as const;
};

const historyValue = (row: ContactHistoryItem, key: string) => {
  if (key === "action") return row.action;
  if (key === "changedAt") return row.changedAt;
  if (key === "source") return row.source;
  if (key === "actionBy") return row.actionBy;
  if (key === "fieldName") return row.fieldName;
  if (key === "contactCode") return row.data?.contactCode ?? row.contactCode;
  return row.data?.[key];
};

export default function ContactHistoryDialog({
  open,
  contactCode,
  items,
  tableConfig,
  loading = false,
  error = "",
  onClose,
}: Props) {
  const columns = useMemo<MRT_ColumnDef<ContactHistoryItem>[]>(
    () =>
      tableConfig.map((config) => {
        const base: MRT_ColumnDef<ContactHistoryItem> = {
          id: config.key,
          header: config.header,
          size: config.size,
          enableSorting: config.sortable ?? false,
          accessorFn: (row) => historyValue(row, config.key),
        };

        if (config.key === "action") {
          return {
            ...base,
            Cell: ({ row }) => (
              <Chip
                size="small"
                color={actionColor(row.original.action)}
                label={actionLabel(row.original.action)}
              />
            ),
          };
        }

        if (config.key === "status") {
          return {
            ...base,
            Cell: ({ row }) => {
              const statusValue = historyValue(row.original, "status");
              if (statusValue === null || statusValue === undefined || String(statusValue).trim() === "") return "N/A";
              const inactive = statusValue === true || String(statusValue).toLowerCase() === "true";
              return (
                <Chip
                  size="small"
                  color={inactive ? "error" : "success"}
                  label={inactive ? "Inactive" : "Active"}
                />
              );
            },
          };
        }

        if (
          config.key === "changedAt" ||
          config.key === "createdAt" ||
          config.key === "updatedAt"
        ) {
          return {
            ...base,
            Cell: ({ row }) => dateTime(historyValue(row.original, config.key)),
          };
        }

        if (config.key === "photo" || config.key === "photoUuid") {
          return {
            ...base,
            size: 72,
            Cell: ({ row }) => {
              const fileName = String(
                historyValue(row.original, config.key) ??
                historyValue(row.original, "photoUuid") ??
                "",
              ).trim();
              if (!fileName) return "N/A";
              return (
                <Tooltip title={fileName} arrow enterTouchDelay={0} leaveTouchDelay={2500}>
                  <IconButton
                    size="small"
                    aria-label={fileName}
                    sx={{ color: "#0f766e", p: 0.35 }}
                  >
                    <PhotoOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              );
            },
          };
        }

        if (config.key === "documents" || config.key === "documentUuids") {
          return {
            ...base,
            size: 110,
            Cell: ({ row }) => {
              const fileValues =
                historyValue(row.original, config.key) ??
                historyValue(row.original, "documentUuids");
              const files = Array.isArray(fileValues)
                ? fileValues.map((fileValue) => String(fileValue).trim()).filter(Boolean)
                : [];
              if (!files.length) return "N/A";
              return (
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 0.25,
                    flexWrap: "wrap",
                    maxWidth: 96,
                  }}
                >
                  {files.map((fileName, index) => (
                    <Tooltip
                      key={`${fileName}-${index}`}
                      title={fileName}
                      arrow
                      enterTouchDelay={0}
                      leaveTouchDelay={2500}
                    >
                      <IconButton
                        size="small"
                        aria-label={fileName}
                        sx={{ color: "#0f766e", p: 0.35 }}
                      >
                        <DescriptionOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  ))}
                </Box>
              );
            },
          };
        }

        return {
          ...base,
          Cell: ({ row }) => {
            const rawValue = historyValue(row.original, config.key);
            if (Array.isArray(rawValue)) {
              const displayValue = textValue(rawValue);
              return (
                <Tooltip
                  title={displayValue === "N/A" ? "" : displayValue}
                  arrow
                  enterTouchDelay={0}
                  leaveTouchDelay={2500}
                >
                  <Box
                    sx={{
                      maxWidth: Math.max(120, config.size ?? 160),
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                      lineHeight: 1.25,
                    }}
                  >
                    {displayValue}
                  </Box>
                </Tooltip>
              );
            }
            return (
              <Box sx={{ whiteSpace: "normal", wordBreak: "break-word", lineHeight: 1.25 }}>
                {textValue(rawValue)}
              </Box>
            );
          },
        };
      }),
    [tableConfig],
  );

  return (
    <Dialog
      open={open}
      onClose={() => undefined}
      fullWidth
      maxWidth="xl"
      slotProps={{ paper: { sx: { maxHeight: "88vh", overflow: "hidden" } } }}
    >
      <DialogTitle
        sx={{
          bgcolor: "#0f9187",
          color: "#fff",
          fontWeight: 700,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          py: 1,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <HistoryOutlinedIcon />
          Contact History - {contactCode}
        </Box>
        <IconButton
          size="small"
          onClick={onClose}
          sx={{
            color: "#fff",
            bgcolor: "#dc2626",
            border: "1px solid #b91c1c",
            "&:hover": { bgcolor: "#b91c1c" },
          }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 1.25, overflow: "hidden" }}>
        {error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}
        {!loading && !error && items.length === 0 && (
          <Alert severity="info" sx={{ mb: 1 }}>
            No history found for this contact.
          </Alert>
        )}
        <ReactTable
          columns={columns}
          data={items}
          loading={loading}
          rowCount={items.length}
          maxHeight={470}
          defaultPageSize={10}
        />
      </DialogContent>
    </Dialog>
  );
}
