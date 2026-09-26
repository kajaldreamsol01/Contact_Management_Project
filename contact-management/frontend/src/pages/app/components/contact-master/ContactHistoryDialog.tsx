import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import type { MRT_ColumnDef } from "material-react-table";
import ReactTable from "../ReactTable";
import {
  getContactHistory,
  getContactHistoryTableConfig,
  type ContactHistoryItem,
  type ContactTableColumnConfig,
} from "./apis";

type Props = {
  open: boolean;
  contactId: number | null;
  contactCode: string;
  onClose: () => void;
};

const value = (data: unknown) => String(data ?? "").trim() || "N/A";

const dateTime = (data: unknown) => {
  if (!data) return "N/A";
  const date = new Date(String(data));
  return Number.isNaN(date.getTime())
    ? value(data)
    : date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
};

const actionLabel = (action: string) =>
  ({
    CREATED: "Created",
    UPDATED: "Updated",
    INACTIVATED: "Inactive",
    REACTIVATED: "Active",
  })[action] ?? action.replaceAll("_", " ");

const actionColor = (action: string) =>
  action === "CREATED" || action === "REACTIVATED"
    ? "success"
    : action === "INACTIVATED"
      ? "error"
      : "info";

const historyValue = (row: ContactHistoryItem, key: string) => {
  if (key === "action") return row.action;
  if (key === "actionBy") return row.actionBy;
  if (key === "changedAt") return row.changedAt;
  if (key === "source") return row.source;
  if (key === "fieldName") return row.fieldName;
  if (key === "contactCode") return row.data?.contactCode ?? row.contactCode;
  return row.data?.[key];
};

const displayHistoryValue = (data: unknown) => {
  if (Array.isArray(data))
    return data.length ? data.map((item) => value(item)).join(", ") : "N/A";
  return value(data);
};

export default function ContactHistoryDialog({
  open,
  contactId,
  contactCode,
  onClose,
}: Props) {
  const [items, setItems] = useState<ContactHistoryItem[]>([]);
  const [config, setConfig] = useState<ContactTableColumnConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !contactId) return;
    let active = true;

    setLoading(true);
    setError("");

    Promise.all([
      getContactHistory(contactId),
      getContactHistoryTableConfig(),
    ])
      .then(([history, columns]) => {
        if (!active) return;
        setItems(history);
        setConfig(columns);
      })
      .catch((e) => {
        if (active)
          setError(e instanceof Error ? e.message : "Unable to load contact history");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [open, contactId]);

  const columns = useMemo<MRT_ColumnDef<ContactHistoryItem>[]>(
    () =>
      config.map((item) => {
        const column: MRT_ColumnDef<ContactHistoryItem> = {
          id: item.key,
          header: item.header,
          size: item.size,
          enableSorting: item.sortable ?? false,
          accessorFn: (row) => historyValue(row, item.key),
        };

        if (item.key === "action") {
          column.Cell = ({ row }) => (
            <Chip
              size="small"
              color={actionColor(row.original.action)}
              label={actionLabel(row.original.action)}
            />
          );
        } else if (item.key === "changedAt") {
          column.Cell = ({ row }) => dateTime(row.original.changedAt);
        } else {
          column.Cell = ({ row }) => (
            <Box sx={{ whiteSpace: "normal", wordBreak: "break-word", lineHeight: 1.25 }}>
              {displayHistoryValue(historyValue(row.original, item.key))}
            </Box>
          );
        }

        return column;
      }),
    [config]
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

        <ReactTable
          columns={columns}
          data={items}
          loading={loading}
          rowCount={items.length}
          maxHeight={390}
          defaultPageSize={10}
        />
      </DialogContent>
    </Dialog>
  );
}
