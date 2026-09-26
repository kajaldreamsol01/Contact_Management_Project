import { useMemo, useState } from "react";
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
  pagination = { pageIndex: 0, pageSize: 5 },
  sorting = [],
  onPaginationChange,
  onSortingChange,
  paginationEnabled = true,
  manualPagination,
  manualSorting,
  maxHeight = 430,
  defaultPageSize = 5,
}: Props<T>) {
  const [internalPagination, setInternalPagination] =
    useState<MRT_PaginationState>({
      pageIndex: 0,
      pageSize: defaultPageSize,
    });

  const [internalSorting, setInternalSorting] =
    useState<MRT_SortingState>(sorting);

  const normalizedColumns = useMemo(
    () =>
      columns.map((column) => {
        if (column.Cell) return column;
        return {
          ...column,
          Cell: ({ cell }: any) => {
            const value = cell.getValue();
            if (value == null) return "N/A";
            if (Array.isArray(value)) return value.length ? value.join(", ") : "N/A";
            if (typeof value === "string" && !value.trim()) return "N/A";
            return String(value);
          },
        };
      }),
    [columns]
  );

  const controlledPagination = Boolean(onPaginationChange);
  const controlledSorting = Boolean(onSortingChange);

  const activePagination = controlledPagination
    ? pagination
    : internalPagination;

  const activeSorting = controlledSorting
    ? sorting
    : internalSorting;

  const totalRows = rowCount ?? data.length;
  const rowsPerPageOptions = useMemo(() => {
    const standard = [5, 10, 15, 20, 25, 50, 100, 200, 500, 1000];
    const values = standard.filter((value) => value < totalRows);
    if (totalRows > 0) values.push(totalRows);
    return [...new Set(values)].sort((a, b) => a - b);
  }, [totalRows]);

  const table = useMaterialReactTable({
    columns: normalizedColumns,
    data,
    rowCount: totalRows,

    state: {
      pagination: activePagination,
      sorting: activeSorting,
      isLoading: loading,
    },

    manualPagination: manualPagination ?? (controlledPagination && rowCount !== undefined),
    manualSorting: manualSorting ?? controlledSorting,
    enablePagination: paginationEnabled,
    enableSorting: true,

    // No refresh/top toolbar icon.
    enableTopToolbar: false,
    enableToolbarInternalActions: false,
    enableColumnActions: false,
    enableColumnFilters: false,
    enableGlobalFilter: false,
    enableDensityToggle: false,
    enableHiding: false,
    enableFullScreenToggle: false,

    enableStickyHeader: true,
    renderFallbackValue: "N/A",
    autoResetPageIndex: false,

    defaultColumn: {
      minSize: 55,
      size: 90,
      maxSize: 160,
    },

    onPaginationChange: (updater) => {
      const next =
        typeof updater === "function"
          ? updater(activePagination)
          : updater;

      if (onPaginationChange) onPaginationChange(next);
      else setInternalPagination(next);
    },

    onSortingChange: (updater) => {
      const next =
        typeof updater === "function"
          ? updater(activeSorting)
          : updater;

      if (onSortingChange) onSortingChange(next);
      else setInternalSorting(next);
    },

    muiPaginationProps: {
      rowsPerPageOptions,
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

    muiTableHeadCellProps: {
      sx: {
        bgcolor: "#0f766e !important",
        color: "#fff !important",
        fontWeight: 700,
        fontSize: 14,
        py: 0.55,
        px: 0.4,
        whiteSpace: "normal",
        lineHeight: 1.1,
        overflow: "hidden",

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
        fontSize: 12,
        py: 0.5,
        px: 0.4,
        verticalAlign: "top",
        overflow: "hidden",
      },
    },

    muiTableBodyRowProps: {
      sx: {
        "&:hover td": {
          bgcolor: "#f0fdfa",
        },
      },
    },

    muiBottomToolbarProps: {
      sx: {
        minHeight: 46,
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
