import { useState } from "react";
import {
  MaterialReactTable,
  useMaterialReactTable,
  type MRT_ColumnDef,
  type MRT_PaginationState,
  type MRT_SortingState,
} from "material-react-table";

type Props<T extends Record<string, any>> = {
  columns: MRT_ColumnDef<T>[];
  data: T[];
  rowCount?: number;
  loading?: boolean;
  pagination?: MRT_PaginationState;
  sorting?: MRT_SortingState;
  onPaginationChange?: (value: MRT_PaginationState) => void;
  onSortingChange?: (value: MRT_SortingState) => void;
  paginationEnabled?: boolean;
  manualPagination?: boolean;
  manualSorting?: boolean;
  maxHeight?: number;
  defaultPageSize?: number;
};

function ReactTable<T extends Record<string, any>>({
  columns,
  data,
  rowCount,
  loading = false,
  pagination = { pageIndex: 0, pageSize: 10 },
  sorting = [],
  onPaginationChange,
  onSortingChange,
  paginationEnabled = true,
  manualPagination = false,
  manualSorting = false,
  maxHeight = 430,
  defaultPageSize = 10,
}: Props<T>) {
  const [internalPagination, setInternalPagination] =
    useState<MRT_PaginationState>({
      pageIndex: 0,
      pageSize: defaultPageSize,
    });

  const [internalSorting, setInternalSorting] =
    useState<MRT_SortingState>(sorting);

  const controlledPagination = Boolean(onPaginationChange);
  const controlledSorting = Boolean(onSortingChange);

  const activePagination = controlledPagination
    ? pagination
    : internalPagination;

  const activeSorting = controlledSorting
    ? sorting
    : internalSorting;

  const table = useMaterialReactTable({
    columns,
    data,
    rowCount: rowCount ?? data.length,
    state: {
      pagination: activePagination,
      sorting: activeSorting,
      isLoading: loading,
    },
    manualPagination,
    manualSorting,
    enablePagination: paginationEnabled,
    enableSorting: true,
    enableTopToolbar: false,
    enableToolbarInternalActions: false,
    enableColumnActions: false,
    enableColumnFilters: false,
    enableGlobalFilter: false,
    enableDensityToggle: false,
    enableHiding: false,
    enableFullScreenToggle: false,
    enableStickyHeader: true,
    autoResetPageIndex: false,
    onPaginationChange: (updater) => {
      const next =
        typeof updater === "function"
          ? updater(activePagination)
          : updater;

      if (onPaginationChange) {
        onPaginationChange(next);
      } else {
        setInternalPagination(next);
      }
    },
    onSortingChange: (updater) => {
      const next =
        typeof updater === "function"
          ? updater(activeSorting)
          : updater;

      if (onSortingChange) {
        onSortingChange(next);
      } else {
        setInternalSorting(next);
      }
    },
    muiPaginationProps: {
      rowsPerPageOptions: [5, 10, 15, 20, 25, 50, 100],
      showFirstButton: true,
      showLastButton: true,
    },
    muiTablePaperProps: {
      sx: {
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
        borderRadius: 1,
        boxShadow: "none",
        border: "1px solid #0f766e",
        bgcolor: "#fff",
      },
    },
    muiTableContainerProps: {
      sx: {
        maxHeight,
        minHeight: 0,
        overflowX: "auto",
        overflowY: "auto",
        flex: "1 1 auto",
      },
    },
    muiTableProps: {
      sx: {
        height: "max-content",
      },
    },
    muiTableBodyProps: {
      sx: {
        height: "max-content",
      },
    },
    muiTableHeadCellProps: {
      sx: {
        bgcolor: "#0f766e !important",
        color: "#fff !important",
        fontWeight: 700,
        fontSize: 15,
        py: 0.55,
        px: 0.45,
        whiteSpace: "normal",
        lineHeight: 1.15,
        "& .Mui-TableHeadCell-Content-Wrapper": {
          color: "#fff !important",
        },
        "& .MuiTableSortLabel-root": {
          color: "#fff !important",
        },
        "& .MuiTableSortLabel-icon": {
          color: "#fff !important",
        },
      },
    },
    muiTableBodyCellProps: {
      sx: {
        fontSize: 13.5,
        py: 0.4,
        px: 0.45,
        verticalAlign: "top",
      },
    },
    muiTableBodyRowProps: {
      sx: {
        height: "auto",
        "& td": {
          height: "auto",
        },
        "&:hover td": {
          bgcolor: "#f0fdfa",
        },
      },
    },
    muiBottomToolbarProps: {
      sx: {
        minHeight: 48,
        bgcolor: "#0f766e",
        color: "#fff",
        "& .MuiTablePagination-root": {
          color: "#fff",
        },
        "& .MuiTablePagination-selectIcon": {
          color: "#fff",
        },
        "& .MuiIconButton-root": {
          color: "#fff !important",
        },
        "& .Mui-disabled": {
          color: "rgba(255,255,255,.35) !important",
        },
      },
    },
  });

  return <MaterialReactTable table={table} />;
}

export default ReactTable;