import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  ButtonBase,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  Paper,
  Tooltip,
  Typography,
} from "@mui/material";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import BlockOutlinedIcon from "@mui/icons-material/BlockOutlined";
import DonutLargeOutlinedIcon from "@mui/icons-material/DonutLargeOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import type {
  MRT_ColumnDef,
  MRT_PaginationState,
  MRT_SortingState,
} from "material-react-table";
import ReactTable from "./app/components/ReactTable";
import ContactHistoryDialog from "./app/components/contact-master/ContactHistoryDialog";
import DateRangeField from "./app/components/DateRangeField";
import BarChartOutlinedIcon from "@mui/icons-material/BarChartOutlined";
import TableChartOutlinedIcon from "@mui/icons-material/TableChartOutlined";
import {
  getContactAnalytics,
  getStatusCount,
  filterContactPage,
  getContactTableConfig,
  type ContactAnalytics,
  type ContactListItem,
  type ContactSearchParams,
  type ContactTableColumnConfig,
} from "./app/components/contact-master/apis";

declare global {
  interface Window {
    am5?: any;
    am5xy?: any;
    am5radar?: any;
    am5themes_Animated?: any;
    Chart?: any;
  }
}

type DateRange = { fromDate: string; toDate: string };
type DetailSelection = { title: string; filters: ContactSearchParams; allTime?: boolean };
type ViewMode = "graph" | "table";

const UI_PRIMARY = "#0f766e";
const UI_PRIMARY_DARK = "#0b5f59";
const UI_PRIMARY_LIGHT = "#147d71";

const ACTIVE_COLOR = "#15803d";
const ACTIVE_DARK = "#14532d";
const ACTIVE_LIGHT = "#116d31";

const INACTIVE_COLOR = "#dc2626";
const INACTIVE_DARK = "#991b1b";
const INACTIVE_LIGHT = "#a31c1c";

const RATIO_COLOR = "#6a9bde";
const RATIO_DARK = "#3a83dd";
const RATIO_LIGHT = "#497cdb";

const formatDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const defaultRange = (): DateRange => {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 6);
  return { fromDate: formatDate(from), toDate: formatDate(to) };
};

const formatDateTime = (value?: string | null) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "N/A";
  const dd = String(date.getDate()).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const yy = String(date.getFullYear()).slice(-2);
  const hh = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");
  const sec = String(date.getSeconds()).padStart(2, "0");
  return `${dd}/${mm}/${yy} ${hh}:${min}:${sec}`;
};

const loadScript = (src: string) =>
  new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`);
    if (existing) {
      if (existing.dataset.loaded === "true") resolve();
      else existing.addEventListener("load", () => resolve(), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = () => {
      script.dataset.loaded = "true";
      resolve();
    };
    script.onerror = () => reject(new Error(`Unable to load ${src}`));
    document.head.appendChild(script);
  });

const AM_SCRIPTS = [
  "https://cdn.amcharts.com/lib/5/index.js",
  "https://cdn.amcharts.com/lib/5/xy.js",
  "https://cdn.amcharts.com/lib/5/radar.js",
  "https://cdn.amcharts.com/lib/5/themes/Animated.js",
];

const CHART_JS = "https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js";

function CountLink({ value, onClick }: { value: number; onClick: () => void }) {
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    if (value <= 0) return;
    window.requestAnimationFrame(onClick);
  };

  return (
    <ButtonBase
      component="button"
      type="button"
      disabled={value <= 0}
      onClick={handleClick}
      aria-label={`Open ${value} contact records`}
      sx={{
        minWidth: 32,
        px: 0.5,
        color: value > 0 ? "#1565c0" : "text.disabled",
        fontWeight: 800,
        textDecoration: value > 0 ? "underline" : "none",
        cursor: value > 0 ? "pointer" : "default",
        borderRadius: 1,
        "&:hover": value > 0 ? { bgcolor: "#e3f2fd" } : undefined,
      }}
    >
      {value}
    </ButtonBase>
  );
}

type SummaryRow = Record<string, string | number>;

function SummaryTable({
  columns,
  data,
  maxHeight = 225,
}: {
  columns: MRT_ColumnDef<SummaryRow>[];
  data: SummaryRow[];
  maxHeight?: number;
}) {
  const [pagination, setPagination] = useState<MRT_PaginationState>({
    pageIndex: 0,
    pageSize: 5,
  });

  useEffect(() => {
    setPagination({
      pageIndex: 0,
      pageSize: 5,
    });
  }, [data]);

  return (
    <ReactTable
      columns={columns}
      data={data}
      rowCount={data.length}
      pagination={pagination}
      onPaginationChange={setPagination}
      paginationEnabled
      manualPagination={false}
      defaultPageSize={5}
      maxHeight={maxHeight}
    />
  );
}

function AnalyticsCard({
  title,
  view,
  onToggle,
  children,
}: {
  title: string;
  view: ViewMode;
  onToggle: () => void;
  children: React.ReactNode;
}) {
  return (
    <Box
      sx={{
        border: "1px solid #e2e8f0",
        borderRadius: 2,
        p: 1.5,
        height: "100%",
        boxSizing: "border-box",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 0.75 }}>
        <Typography variant="body2" sx={{ fontWeight: 700 }}>
          {title}
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
          <Tooltip title="Graph View">
            <IconButton
              size="small"
              aria-label="Show graph view"
              onClick={() => view === "table" && onToggle()}
              sx={{
                border: "1px solid",
                borderColor: view === "graph" ? "#0f766e" : "#cbd5e1",
                bgcolor: view === "graph" ? "#e6fffb" : "#fff",
                color: view === "graph" ? "#0f766e" : "#64748b",
                borderRadius: 1.25,
                width: 30,
                height: 30,
              }}
            >
              <BarChartOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Table View">
            <IconButton
              size="small"
              aria-label="Show table view"
              onClick={() => view === "graph" && onToggle()}
              sx={{
                border: "1px solid",
                borderColor: view === "table" ? "#0f766e" : "#cbd5e1",
                bgcolor: view === "table" ? "#e6fffb" : "#fff",
                color: view === "table" ? "#0f766e" : "#64748b",
                borderRadius: 1.25,
                width: 30,
                height: 30,
              }}
            >
              <TableChartOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>
      {children}
    </Box>
  );
}

function DetailTableDialog({
  selection,
  dateRange,
  onClose,
}: {
  selection: DetailSelection | null;
  dateRange: DateRange;
  onClose: () => void;
}) {
  const [data, setData] = useState<ContactListItem[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [pagination, setPagination] = useState<MRT_PaginationState>({
    pageIndex: 0,
    pageSize: 5,
  });
  const [sorting, setSorting] = useState<MRT_SortingState>([]);
  const [tableConfig, setTableConfig] = useState<ContactTableColumnConfig[]>([]);
  const [historyContact, setHistoryContact] = useState<{ id: number; contactCode: string } | null>(null);

  const selectionKey = useMemo(
    () => selection ? JSON.stringify({ filters: selection.filters, allTime: selection.allTime }) : "",
    [selection]
  );

  useEffect(() => {
    let cancelled = false;
    getContactTableConfig()
      .then((config) => {
        if (!cancelled) setTableConfig(config);
      })
      .catch(() => {
        if (!cancelled) setTableConfig([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selection) return;

    let cancelled = false;
    const sort = sorting[0];

    setLoading(true);
    setError("");

    filterContactPage({
      ...selection.filters,
      fromDate: selection.allTime ? undefined : dateRange.fromDate,
      toDate: selection.allTime ? undefined : dateRange.toDate,
      page: pagination.pageIndex,
      size: pagination.pageSize,
      sort: sort?.id || "id",
      direction: sort?.desc === false ? "asc" : "desc",
    })
      .then((page) => {
        if (cancelled) return;
        setData(page.content);
        setRowCount(page.totalElements);
      })
      .catch((e) => {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : "Unable to load contact details");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [
    dateRange.fromDate,
    dateRange.toDate,
    pagination.pageIndex,
    pagination.pageSize,
    selectionKey,
    sorting,
  ]);

  useEffect(() => {
    setPagination({ pageIndex: 0, pageSize: 5 });
    setSorting([]);
  }, [selectionKey]);

  const columns = useMemo<MRT_ColumnDef<ContactListItem>[]>(() => {
    const baseColumns: MRT_ColumnDef<ContactListItem>[] = [
      {
        accessorKey: "contactCode",
        header: "Contact Code",
        size: 120,
        Cell: ({ row }) => {
          const code = row.original.contactCode;
          if (!code) return "N/A";

          return (
            <ButtonBase
              component="button"
              type="button"
              title="View contact history"
              onClick={(event) => {
                event.preventDefault();
                event.stopPropagation();
                setHistoryContact({
                  id: Number(row.original.id),
                  contactCode: String(code),
                });
              }}
              sx={{
                p: 0,
                minWidth: 0,
                color: "#1976d2",
                fontSize: 12.5,
                fontWeight: 800,
                lineHeight: 1.25,
                textDecoration: "underline",
                textUnderlineOffset: "2px",
                cursor: "pointer",
                borderRadius: 0.5,
                "&:hover": {
                  color: "#0d47a1",
                  bgcolor: "#eff6ff",
                  textDecoration: "underline",
                },
              }}
            >
              {String(code)}
            </ButtonBase>
          );
        },
      },
      { accessorKey: "name", header: "Name", size: 150 },
      { accessorKey: "contactType", header: "Contact Type", size: 110 },
      { accessorKey: "department", header: "Department", size: 120 },
      { accessorKey: "designation", header: "Designation", size: 130 },
      { accessorKey: "companyName", header: "Company", size: 140 },
      { accessorKey: "mobile", header: "Mobile", size: 115 },
      { accessorKey: "alternateMobile", header: "Alternate Mobile", size: 130 },
      { accessorKey: "officeNumber", header: "Office Number", size: 120 },
      { accessorKey: "email", header: "Email", size: 190 },
      { accessorKey: "alternateEmail", header: "Alternate Email", size: 190 },
      { accessorKey: "employeeId", header: "Employee ID", size: 110 },
      { accessorKey: "gender", header: "Gender", size: 90 },
      { accessorKey: "maritalStatus", header: "Marital Status", size: 110 },
      { accessorKey: "dateOfBirth", header: "DOB", size: 110 },
      { accessorKey: "bloodGroup", header: "Blood Group", size: 100 },
      { accessorKey: "country", header: "Country", size: 110 },
      { accessorKey: "state", header: "State", size: 110 },
      { accessorKey: "city", header: "City", size: 110 },
      { accessorKey: "address", header: "Address", size: 220 },
      { accessorKey: "pinCode", header: "Pin Code", size: 90 },
      { accessorKey: "remarks", header: "Remarks", size: 180 },
      {
        accessorKey: "status",
        header: "Status",
        size: 90,
        Cell: ({ row }) => (row.original.status ? "Inactive" : "Active"),
      },
      {
        accessorKey: "createdAt",
        header: "Created On",
        size: 150,
        Cell: ({ cell }) => {
          const value = cell.getValue<string>();
          return formatDateTime(value);
        },
      },
    ];

    if (!tableConfig.length) return baseColumns;

    const normalize = (value: string) =>
      value.replace(/[^a-z0-9]/gi, "").toLowerCase();

    const configByKey = new Map(
      tableConfig.map((item) => [normalize(item.key), item])
    );

    return baseColumns
      .map((column, index) => {
        const key = String(
          (column as any).id ?? (column as any).accessorKey ?? ""
        );
        const config = configByKey.get(normalize(key));

        if (!config) {
          return { column, index, order: Number.MAX_SAFE_INTEGER };
        }

        if (config.visible === false) return null;

        return {
          column: {
            ...column,
            header: config.header || String((column as any).header || key),
          },
          index,
          order: Number.isFinite(config.order)
            ? Number(config.order)
            : index,
        };
      })
      .filter(Boolean)
      .sort((a: any, b: any) => a.order - b.order || a.index - b.index)
      .map((item: any) => item.column);
  }, [tableConfig]);

  return (
    <>
    <Dialog
      open={Boolean(selection)}
      onClose={(_, reason) => {
        // Detail popup must close only from the red X button.
        if (reason === "backdropClick" || reason === "escapeKeyDown") return;
      }}
      fullWidth
      maxWidth="xl"
      disableRestoreFocus
      keepMounted={false}
      slotProps={{
        paper: {
          sx: {
            maxHeight: "88vh",
            overflow: "hidden",
            border: `1px solid ${UI_PRIMARY}`,
            borderRadius: 2,
          },
        },
      }}
    >
      <DialogTitle
        sx={{
          py: 1.25,
          pr: 6,
          fontSize: 16,
          fontWeight: 800,
          bgcolor: UI_PRIMARY,
          color: "#fff",
        }}
      >
        {selection?.title || "Contact Details"}
        <IconButton
          title="Close"
          aria-label="Close contact details"
          onClick={onClose}
          sx={{
            position: "absolute",
            right: 10,
            top: 7,
            bgcolor: "#dc2626",
            color: "#fff",
            width: 30,
            height: 30,
            "&:hover": { bgcolor: "#b91c1c" },
          }}
        >
          <CloseOutlinedIcon fontSize="small" />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers sx={{ p: 1.5, overflow: "hidden" }}>
        {error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}
        <ReactTable
          columns={columns}
          data={data}
          rowCount={rowCount}
          loading={loading}
          pagination={pagination}
          sorting={sorting}
          onPaginationChange={setPagination}
          onSortingChange={setSorting}
          manualPagination
          manualSorting
          maxHeight={480}
        />
      </DialogContent>
    </Dialog>

    <ContactHistoryDialog
      open={Boolean(historyContact)}
      contactId={historyContact?.id ?? null}
      contactCode={historyContact?.contactCode ?? ""}
      onClose={() => setHistoryContact(null)}
    />
    </>
  );
}

function AmChartsPanel({
  data,
  onOpenDetails,
}: {
  data: ContactAnalytics;
  onOpenDetails: (selection: DetailSelection) => void;
}) {
  const barRef = useRef<HTMLDivElement | null>(null);
  const gaugeRef = useRef<HTMLDivElement | null>(null);
  const stackRef = useRef<HTMLDivElement | null>(null);
  const [error, setError] = useState("");
  const [views, setViews] = useState<Record<string, ViewMode>>({
    department: "graph",
    active: "graph",
    stacked: "graph",
  });

  const contactTypes = useMemo(
    () =>
      (data.contactTypes || [])
        .filter((item) => item.value > 0)
        .sort((a, b) => b.value - a.value)
        .slice(0, 8),
    [data.contactTypes]
  );
  const stackData = useMemo(
    () =>
      (data.contactTypeStatus || [])
        .filter((item) => item.active > 0 || item.inactive > 0)
        .sort((a, b) => b.active + b.inactive - (a.active + a.inactive))
        .slice(0, 8),
    [data.contactTypeStatus]
  );

  const toggle = (key: string) =>
    setViews((current) => ({ ...current, [key]: current[key] === "graph" ? "table" : "graph" }));

  useEffect(() => {
    let disposed = false;
    let barRoot: any;
    let gaugeRoot: any;
    let stackRoot: any;

    const render = async () => {
      try {
        for (const src of AM_SCRIPTS) await loadScript(src);
        if (disposed || !barRef.current || !gaugeRef.current || !stackRef.current) return;

        const { am5, am5xy, am5radar, am5themes_Animated } = window;
        if (!am5 || !am5xy || !am5radar) throw new Error("AmCharts is unavailable");

        barRoot = am5.Root.new(barRef.current);
        if (am5themes_Animated) barRoot.setThemes([am5themes_Animated.new(barRoot)]);
        const barChart = barRoot.container.children.push(am5xy.XYChart.new(barRoot, {
          panX: false, panY: false, wheelX: "none", wheelY: "none", paddingLeft: 0,
        }));
        const xRenderer = am5xy.AxisRendererX.new(barRoot, { minGridDistance: 28 });
        xRenderer.labels.template.setAll({ rotation: -28, centerY: am5.p50, centerX: am5.p100, fontSize: 11 });
        const xAxis = barChart.xAxes.push(am5xy.CategoryAxis.new(barRoot, { categoryField: "label", renderer: xRenderer }));
        const yAxis = barChart.yAxes.push(am5xy.ValueAxis.new(barRoot, { min: 0, extraMax: 0.15, renderer: am5xy.AxisRendererY.new(barRoot, {}) }));
        const barSeries = barChart.series.push(am5xy.ColumnSeries.new(barRoot, {
          name: "Contacts", xAxis, yAxis, valueYField: "value", categoryXField: "label",
        }));
        barSeries.columns.template.setAll({
          tooltipText: "{categoryX}: {valueY}", cornerRadiusTL: 7, cornerRadiusTR: 7,
          fill: am5.color(0x0f9187), stroke: am5.color(0x0f9187), width: am5.percent(64),
        });
        barSeries.bullets.push(() => am5.Bullet.new(barRoot, {
          locationY: 1,
          sprite: am5.Label.new(barRoot, {
            text: "{valueY}", populateText: true, centerX: am5.p50, centerY: am5.p100,
            dy: -8, fontSize: 11, fontWeight: "600",
          }),
        }));
        xAxis.data.setAll(contactTypes);
        barSeries.data.setAll(contactTypes);
        barSeries.appear(500);
        barChart.appear(500, 50);

        gaugeRoot = am5.Root.new(gaugeRef.current);
        if (am5themes_Animated) gaugeRoot.setThemes([am5themes_Animated.new(gaugeRoot)]);
        const activePercent = data.total ? Math.round((data.active / data.total) * 100) : 0;
        const gaugeChart = gaugeRoot.container.children.push(am5radar.RadarChart.new(gaugeRoot, {
          panX: false, panY: false, startAngle: 180, endAngle: 360,
        }));
        const gaugeRenderer = am5radar.AxisRendererCircular.new(gaugeRoot, { innerRadius: -24, strokeOpacity: 0 });
        gaugeRenderer.labels.template.set("forceHidden", true);
        gaugeRenderer.grid.template.set("forceHidden", true);
        gaugeRenderer.ticks.template.set("forceHidden", true);
        const gaugeAxis = gaugeChart.xAxes.push(am5xy.ValueAxis.new(gaugeRoot, {
          min: 0, max: 100, strictMinMax: true, renderer: gaugeRenderer,
        }));
        const backgroundRange = gaugeAxis.makeDataItem({ value: 0, endValue: 100 });
        gaugeAxis.createAxisRange(backgroundRange);
        backgroundRange.get("axisFill")?.setAll({ visible: true, fill: am5.color(0xe2e8f0), fillOpacity: 1 });
        const range = gaugeAxis.makeDataItem({ value: 0, endValue: activePercent });
        gaugeAxis.createAxisRange(range);
        range.get("axisFill")?.setAll({ visible: true, fill: am5.color(0x0f9187), fillOpacity: 1 });
        const handItem = gaugeAxis.makeDataItem({ value: activePercent });
        gaugeAxis.createAxisRange(handItem);
        handItem.set("bullet", am5xy.AxisBullet.new(gaugeRoot, {
          sprite: am5radar.ClockHand.new(gaugeRoot, {
            radius: am5.percent(76), pinRadius: 7, bottomWidth: 6,
            fill: am5.color(0x334155), stroke: am5.color(0x334155),
          }),
        }));
        gaugeChart.radarContainer.children.push(am5.Label.new(gaugeRoot, {
          text: `${activePercent}%`, centerX: am5.p50, x: am5.p50, y: am5.percent(70),
          fontSize: 30, fontWeight: "700", fill: am5.color(0x0f172a),
        }));
        gaugeChart.radarContainer.children.push(am5.Label.new(gaugeRoot, {
          text: "Active", centerX: am5.p50, x: am5.p50, y: am5.percent(86),
          fontSize: 12, fill: am5.color(0x64748b),
        }));
        gaugeChart.appear(500, 50);

        stackRoot = am5.Root.new(stackRef.current);
        if (am5themes_Animated) stackRoot.setThemes([am5themes_Animated.new(stackRoot)]);
        const stackChart = stackRoot.container.children.push(am5xy.XYChart.new(stackRoot, {
          panX: false, panY: false, wheelX: "none", wheelY: "none", paddingLeft: 0,
        }));
        const stackXRenderer = am5xy.AxisRendererX.new(stackRoot, { minGridDistance: 30 });
        stackXRenderer.labels.template.setAll({ rotation: -20, centerX: am5.p100, centerY: am5.p50, fontSize: 11 });
        const stackXAxis = stackChart.xAxes.push(am5xy.CategoryAxis.new(stackRoot, { categoryField: "label", renderer: stackXRenderer }));
        const stackYAxis = stackChart.yAxes.push(am5xy.ValueAxis.new(stackRoot, { min: 0, extraMax: 0.12, renderer: am5xy.AxisRendererY.new(stackRoot, {}) }));
        const makeStackSeries = (name: string, field: "active" | "inactive", color: number) => {
          const series = stackChart.series.push(am5xy.ColumnSeries.new(stackRoot, {
            name, stacked: true, xAxis: stackXAxis, yAxis: stackYAxis,
            valueYField: field, categoryXField: "label",
          }));
          series.columns.template.setAll({
            width: am5.percent(58), fill: am5.color(color), stroke: am5.color(color),
            tooltipText: "{categoryX} · " + name + ": {valueY}",
          });
          series.bullets.push(() => am5.Bullet.new(stackRoot, {
            locationY: 0.5,
            sprite: am5.Label.new(stackRoot, {
              text: "{valueY}", populateText: true, centerX: am5.p50, centerY: am5.p50,
              fill: am5.color(0xffffff), fontSize: 11, fontWeight: "700",
            }),
          }));
          series.data.setAll(stackData);
          return series;
        };
        const activeSeries = makeStackSeries("Active", "active", 0x2e7d32);
        const inactiveSeries = makeStackSeries("Inactive", "inactive", 0xd32f2f);
        const stackLegend = stackChart.children.push(am5.Legend.new(stackRoot, { centerX: am5.p50, x: am5.p50, marginTop: 8 }));
        stackLegend.data.setAll([activeSeries, inactiveSeries]);
        stackXAxis.data.setAll(stackData);
        activeSeries.appear(500);
        inactiveSeries.appear(500);
        stackChart.appear(500, 50);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Unable to render AmCharts");
      }
    };

    void render();
    return () => {
      disposed = true;
      barRoot?.dispose?.();
      gaugeRoot?.dispose?.();
      stackRoot?.dispose?.();
    };
  }, [data, contactTypes, stackData]);

  return (
    <Paper elevation={0} sx={{ p: 2.25, border: "1px solid #dbe7e5", borderRadius: 2.5 }}>
      <Typography sx={{ fontWeight: 800, mb: 1.5 }}></Typography>
      {error && <Alert severity="warning" sx={{ mb: 1 }}>{error}</Alert>}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <AnalyticsCard title="Contact Type Distribution" view={views.department} onToggle={() => toggle("department")}>
            <Box ref={barRef} sx={{ height: 300, display: views.department === "graph" ? "block" : "none" }} />
            {views.department === "table" && (
              <SummaryTable
                data={contactTypes.map((item) => ({
                  contactType: item.label,
                  count: item.value,
                  percentage: data.total ? `${Math.round((item.value / data.total) * 100)}%` : "0%",
                }))}
                columns={[
                  { accessorKey: "contactType", header: "Contact Type" },
                  {
                    accessorKey: "count",
                    header: "Count",
                    Cell: ({ row }) => (
                      <CountLink
                        value={Number(row.original.count)}
                        onClick={() => onOpenDetails({
                          title: `${row.original.contactType} Contacts`,
                          filters: { contactType: String(row.original.contactType) },
                        })}
                      />
                    ),
                  },
                  { accessorKey: "percentage", header: "Percentage" },
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <AnalyticsCard title="Active Ratio" view={views.active} onToggle={() => toggle("active")}>
            <Box ref={gaugeRef} sx={{ height: 210, display: views.active === "graph" ? "block" : "none" }} />
            {views.active === "graph" && (
              <Box sx={{ textAlign: "center", mt: -1 }}><Typography variant="caption" color="text.secondary">{data.active} active of {data.total} contacts</Typography></Box>
            )}
            {views.active === "table" && (
              <SummaryTable
                data={[
                  { status: "Active", count: data.active, percentage: data.total ? `${((data.active / data.total) * 100).toFixed(1)}%` : "0%" },
                  { status: "Inactive", count: data.inactive, percentage: data.total ? `${((data.inactive / data.total) * 100).toFixed(1)}%` : "0%" },
                  { status: "Total", count: data.total, percentage: "100%" },
                ]}
                columns={[
                  { accessorKey: "status", header: "Status" },
                  {
                    accessorKey: "count",
                    header: "Count",
                    Cell: ({ row }) => {
                      const status = String(row.original.status);
                      const filters = status === "Active" ? { status: false } : status === "Inactive" ? { status: true } : {};
                      return <CountLink value={Number(row.original.count)} onClick={() => onOpenDetails({ title: `${status} Contacts`, filters })} />;
                    },
                  },
                  { accessorKey: "percentage", header: "Percentage" },
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <AnalyticsCard title="Contact Type Status Stacked" view={views.stacked} onToggle={() => toggle("stacked")}>
            <Box sx={{ height: 320, display: views.stacked === "graph" ? "block" : "none", position: "relative" }}>
              <Box ref={stackRef} sx={{ height: "100%" }} />
              {stackData.length === 0 && (
                <Box sx={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", bgcolor: "#fff", color: "text.secondary", fontSize: 13 }}>
                  No Contact Type status data available
                </Box>
              )}
            </Box>
            {views.stacked === "table" && (
              <SummaryTable
                data={stackData.map((item) => ({
                  contactType: item.label,
                  active: item.active,
                  inactive: item.inactive,
                  total: item.active + item.inactive,
                }))}
                columns={[
                  { accessorKey: "contactType", header: "Contact Type" },
                  {
                    accessorKey: "active",
                    header: "Active",
                    Cell: ({ row }) => <CountLink value={Number(row.original.active)} onClick={() => onOpenDetails({ title: `${row.original.contactType} - Active Contacts`, filters: { contactType: String(row.original.contactType), status: false } })} />,
                  },
                  {
                    accessorKey: "inactive",
                    header: "Inactive",
                    Cell: ({ row }) => <CountLink value={Number(row.original.inactive)} onClick={() => onOpenDetails({ title: `${row.original.contactType} - Inactive Contacts`, filters: { contactType: String(row.original.contactType), status: true } })} />,
                  },
                  {
                    accessorKey: "total",
                    header: "Total",
                    Cell: ({ row }) => <CountLink value={Number(row.original.total)} onClick={() => onOpenDetails({ title: `${row.original.contactType} - All Contacts`, filters: { contactType: String(row.original.contactType) } })} />,
                  },
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>
      </Grid>
    </Paper>
  );
}

function ReactChartPanel({
  data,
  onOpenDetails,
}: {
  data: ContactAnalytics;
  onOpenDetails: (selection: DetailSelection) => void;
}) {
  const mixedRef = useRef<HTMLCanvasElement | null>(null);
  const typeRef = useRef<HTMLCanvasElement | null>(null);
  const donutRef = useRef<HTMLCanvasElement | null>(null);
  const [error, setError] = useState("");
  const [views, setViews] = useState<Record<string, ViewMode>>({
    trend: "graph",
    types: "graph",
    status: "graph",
  });

  const contactTypes = useMemo(
    () =>
      (data.contactTypes || [])
        .filter((item) => item.value > 0)
        .sort((a, b) => b.value - a.value)
        .slice(0, 6),
    [data.contactTypes]
  );

  const cities = useMemo(
    () =>
      (data.cities || [])
        .filter(
          (item) =>
            item.value > 0 &&
            String(item.label || "").trim() &&
            !["Unassigned", "N/A"].includes(String(item.label || "").trim())
        )
        .sort((a, b) => b.value - a.value)
        .slice(0, 8),
    [data.cities]
  );

  const toggle = (key: string) =>
    setViews((current) => ({ ...current, [key]: current[key] === "graph" ? "table" : "graph" }));

  useEffect(() => {
    let mixedChart: any;
    let typeChart: any;
    let donutChart: any;
    let disposed = false;

    const render = async () => {
      try {
        await loadScript(CHART_JS);
        if (disposed || !window.Chart || !mixedRef.current || !typeRef.current || !donutRef.current) return;

        mixedChart = new window.Chart(mixedRef.current, {
          data: {
            labels: cities.map((item) => item.label),
            datasets: [
              {
                type: "bar",
                label: "Contacts",
                data: cities.map((item) => item.value),
                backgroundColor: "#0f9187",
                borderColor: "#0f9187",
                borderWidth: 1,
                borderRadius: 7,
                borderSkipped: false,
                yAxisID: "y",
              },
              {
                type: "line",
                label: "Share %",
                data: contactTypes.map((item) => data.total ? Number(((item.value / data.total) * 100).toFixed(1)) : 0),
                borderColor: "#f59e0b",
                backgroundColor: "#f59e0b",
                pointBackgroundColor: "#f59e0b",
                pointBorderColor: "#ffffff",
                pointBorderWidth: 2,
                borderWidth: 3,
                pointRadius: 4,
                pointHoverRadius: 6,
                tension: 0.35,
                yAxisID: "y1",
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: "index", intersect: false },
            scales: {
              x: { grid: { display: false }, border: { display: false }, ticks: { maxRotation: 25, minRotation: 0 } },
              y: { beginAtZero: true, position: "left", grid: { color: "rgba(148,163,184,.16)" }, border: { display: false }, ticks: { precision: 0 } },
              y1: { beginAtZero: true, position: "right", grid: { drawOnChartArea: false }, border: { display: false }, ticks: { callback: (value: any) => `${value}%` } },
            },
            plugins: {
              legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 9, padding: 16 } },
              tooltip: { callbacks: { label: (context: any) => context.dataset.type === "line" ? ` Share: ${context.raw}%` : ` Contacts: ${context.raw}` } },
            },
          },
        });

    
        typeChart = new window.Chart(typeRef.current, {
          type: "bar",

          data: {
            labels: cities.map(item => item.label),

            datasets: [{
              label: "Contacts",
              data: cities.map(item => item.value),

              backgroundColor: [
                "#0f9187",
                "#22a699",
                "#47b8ad",
                "#6dc8c0",
                "#8fd6d0",
                "#b1e4df",
                "#ccefeb",
                "#ddf6f3",
              ],

              borderColor: [
                "#0f9187",
                "#22a699",
                "#47b8ad",
                "#6dc8c0",
                "#8fd6d0",
                "#b1e4df",
                "#ccefeb",
                "#ddf6f3",
              ],

              borderWidth: 1,
              borderRadius: 7,
              borderSkipped: false,
              barThickness: 16,
              maxBarThickness: 18,
            }],
          },

          options: {
            indexAxis: "y",
            responsive: true,
            maintainAspectRatio: false,

            scales: {
              x: {
                beginAtZero: true,
                grid: {
                  color: "rgba(148,163,184,.14)",
                },
                border: {
                  display: false,
                },
                ticks: {
                  precision: 0,
                },
              },

              y: {
                grid: {
                  display: false,
                },
                border: {
                  display: false,
                },
                ticks: {
                  font: {
                    size: 11,
                    weight: "600",
                  },
                },
              },
            },

            plugins: {
              legend: {
                display: false,
              },

              tooltip: {
                callbacks: {
                  label: (context: any) =>
                    ` ${context.raw} contacts`,
                },
              },
            },
          },
        });

        donutChart = new window.Chart(donutRef.current, {
          type: "doughnut",
          data: {
            labels: ["Active", "Inactive"],
            datasets: [{
              data: [data.active, data.inactive],
              backgroundColor: [ACTIVE_COLOR, INACTIVE_COLOR],
              hoverBackgroundColor: ["#1b5e20", "#b71c1c"],
              borderColor: "#ffffff",
              borderWidth: 3,
              hoverOffset: 5,
            }],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: "68%",
            plugins: {
              legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 9, padding: 14 } },
              tooltip: {
                callbacks: {
                  label: (context: any) => {
                    const value = Number(context.raw || 0);
                    const percent = data.total ? Math.round((value / data.total) * 100) : 0;
                    return ` ${context.label}: ${value} (${percent}%)`;
                  }
                }
              },
            },
          },
          plugins: [{
            id: "donutCenterLabel",
            afterDraw(chartInstance: any) {
              const firstArc = chartInstance.getDatasetMeta(0)?.data?.[0];
              if (!firstArc) return;
              const { ctx } = chartInstance;
              const percent = data.total ? Math.round((data.active / data.total) * 100) : 0;
              ctx.save();
              ctx.textAlign = "center";
              ctx.textBaseline = "middle";
              ctx.fillStyle = "#0f172a";
              ctx.font = "700 24px Arial";
              ctx.fillText(`${percent}%`, firstArc.x, firstArc.y - 5);
              ctx.fillStyle = "#64748b";
              ctx.font = "500 11px Arial";
              ctx.fillText("Active", firstArc.x, firstArc.y + 17);
              ctx.restore();
            },
          }],
        });
      } catch (e) {
        setError(e instanceof Error ? e.message : "Unable to render React charts");
      }
    };

    void render();
    return () => {
      disposed = true;
      mixedChart?.destroy?.();
      typeChart?.destroy?.();
      donutChart?.destroy?.();
    };
  }, [cities, contactTypes, data]);

  return (
    <Paper elevation={0} sx={{ p: 2.25, border: "1px solid #dbe7e5", borderRadius: 2.5 }}>
      <Typography sx={{ fontWeight: 800, mb: 1.5 }}></Typography>
      {error && <Alert severity="warning" sx={{ mb: 1 }}>{error}</Alert>}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}>
          <AnalyticsCard title="Contact Type Trend" view={views.trend} onToggle={() => toggle("trend")}>
            <Box sx={{ height: 300, display: views.trend === "graph" ? "block" : "none" }}><canvas ref={mixedRef} /></Box>
            {views.trend === "table" && (
              <SummaryTable
                data={contactTypes.map((item) => ({ contactType: item.label, contacts: item.value, share: data.total ? `${((item.value / data.total) * 100).toFixed(1)}%` : "0%" }))}
                columns={[
                  { accessorKey: "contactType", header: "Contact Type" },
              
                  {
                    accessorKey: "contacts",
                    header: "Contacts",

                    Cell: ({ row }) => (
                      <CountLink
                        value={Number(row.original.contacts)}
                        onClick={() =>
                          onOpenDetails({
                            title: `${row.original.contactType} Contacts`,
                            filters: {
                              contactType: String(
                                row.original.contactType
                              ),
                            },
                          })
                        }
                      />
                    ),
                  }
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>

      
        <Grid size={{ xs: 12, lg: 7 }}>
          <AnalyticsCard
            title="City Distribution"
            view={views.types}
            onToggle={() => toggle("types")}
          >
            <Box
              sx={{
                height: 250,
                display: views.types === "graph"
                  ? "block"
                  : "none",
                position: "relative",
              }}
            >
              <canvas ref={typeRef} />

              {cities.length === 0 && (
                <Box
                  sx={{
                    position: "absolute",
                    inset: 0,
                    display: "grid",
                    placeItems: "center",
                    bgcolor: "#fff",
                    color: "text.secondary",
                    fontSize: 13,
                  }}
                >
                  No City data available
                </Box>
              )}
            </Box>

            {views.types === "table" && (
              <SummaryTable
                data={cities.map(item => ({
                  city: item.label,
                  count: item.value,
                  share: data.total
                    ? `${((item.value / data.total) * 100).toFixed(1)}%`
                    : "0%",
                }))}

                columns={[
                  {
                    accessorKey: "city",
                    header: "City",
                  },
                  {
                    accessorKey: "count",
                    header: "Count",

                    Cell: ({ row }) => (
                      <CountLink
                        value={Number(row.original.count)}
                        onClick={() =>
                          onOpenDetails({
                            title: `${row.original.city} Contacts`,
                            filters: {
                              city: String(row.original.city),
                            },
                          })
                        }
                      />
                    ),
                  },
                  {
                    accessorKey: "share",
                    header: "Share %",
                  },
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 5 }}>
          <AnalyticsCard title="Contact Status" view={views.status} onToggle={() => toggle("status")}>
            <Box sx={{ height: 250, display: views.status === "graph" ? "block" : "none" }}><canvas ref={donutRef} /></Box>
            {views.status === "table" && (
              <SummaryTable
                data={[
                  { status: "Active", count: data.active, percentage: data.total ? `${((data.active / data.total) * 100).toFixed(1)}%` : "0%" },
                  { status: "Inactive", count: data.inactive, percentage: data.total ? `${((data.inactive / data.total) * 100).toFixed(1)}%` : "0%" },
                ]}
                columns={[
                  { accessorKey: "status", header: "Status" },
                  { accessorKey: "count", header: "Count", Cell: ({ row }) => <CountLink value={Number(row.original.count)} onClick={() => onOpenDetails({ title: `${row.original.status} Contacts`, filters: { status: row.original.status === "Inactive" } })} /> },
                  { accessorKey: "percentage", header: "Percentage" },
                ]}
              />
            )}
          </AnalyticsCard>
        </Grid>
      </Grid>
    </Paper>
  );
}

function Dashboard() {
  const initialRange = useMemo(() => defaultRange(), []);
  const [draftRange, setDraftRange] = useState<DateRange>(initialRange);
  const [appliedRange, setAppliedRange] = useState<DateRange>(initialRange);
  const [data, setData] = useState<ContactAnalytics | null>(null);
  const [counts, setCounts] = useState({ total: 0, active: 0, inactive: 0 });
  const [detailSelection, setDetailSelection] = useState<DetailSelection | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const analyticsRequestKey = useRef("");
  const today = useMemo(() => formatDate(new Date()), []);

  const loadAnalytics = useCallback((force = false) => {
    const requestKey = `${appliedRange.fromDate}|${appliedRange.toDate}`;

    if (!force && analyticsRequestKey.current === requestKey) {
      return;
    }

    analyticsRequestKey.current = requestKey;
    setLoading(true);

    getContactAnalytics({
      fromDate: appliedRange.fromDate,
      toDate: appliedRange.toDate,
    })
      .then((result) => {
        setData(result);
        setError("");
      })
      .catch((e) => {
        analyticsRequestKey.current = "";
        setError(e instanceof Error ? e.message : "Unable to load dashboard");
      })
      .finally(() => setLoading(false));
  }, [appliedRange.fromDate, appliedRange.toDate]);

  const loadCounts = useCallback(async () => {
    try {
      const result = await getStatusCount();
      setCounts({ total: result.active + result.inactive, active: result.active, inactive: result.inactive });
    } catch {
      setCounts({ total: 0, active: 0, inactive: 0 });
    }
  }, []);

  useEffect(() => {

    void loadAnalytics(false);
    void loadCounts();

    const refresh = () => {
      void loadAnalytics(true);
      void loadCounts();
    };

    window.addEventListener("contact-analytics-updated", refresh);

    return () => {
      window.removeEventListener("contact-analytics-updated", refresh);
    };
  }, [loadAnalytics, loadCounts]);

  const stats = useMemo(() => [
    { label:"Total Contacts",value:counts.total,icon:<GroupsOutlinedIcon />,color:UI_PRIMARY,darkColor:UI_PRIMARY_DARK,lightColor:UI_PRIMARY_LIGHT,filters:{},allTime:true },
    { label:"Active Contacts",value:counts.active,icon:<CheckCircleOutlineOutlinedIcon />,color:ACTIVE_COLOR,darkColor:ACTIVE_DARK,lightColor:ACTIVE_LIGHT,filters:{status:false},allTime:true },
    { label:"Inactive Contacts",value:counts.inactive,icon:<BlockOutlinedIcon />,color:INACTIVE_COLOR,darkColor:INACTIVE_DARK,lightColor:INACTIVE_LIGHT,filters:{status:true},allTime:true },
    { label:"Active Ratio",value:`${counts.total ? Math.round((counts.active / counts.total) * 100) : 0}%`,icon:<DonutLargeOutlinedIcon />,color:RATIO_COLOR,darkColor:RATIO_DARK,lightColor:RATIO_LIGHT,filters:null,allTime:true },
  ], [counts]);

  const handleOpenDetails = useCallback((selection: DetailSelection) => {
    const focused = document.activeElement;
    if (focused instanceof HTMLElement) focused.blur();

    window.requestAnimationFrame(() => setDetailSelection(selection));
  }, []);

  const hasRangeChanged =
    draftRange.fromDate !== appliedRange.fromDate ||
    draftRange.toDate !== appliedRange.toDate;

  const handleSearch = () => {
    if (!draftRange.fromDate || !draftRange.toDate) return;
    if (draftRange.fromDate > today || draftRange.toDate > today) return;
    if (draftRange.toDate < draftRange.fromDate) return;
    if (!hasRangeChanged) return;

    setAppliedRange({ ...draftRange });
  };

  const handleReset = () => {
    const next = defaultRange();
    setDraftRange(next);

   
    if (
      appliedRange.fromDate !== next.fromDate ||
      appliedRange.toDate !== next.toDate
    ) {
      setAppliedRange(next);
    }
  };

  if (loading && !data) return <Box sx={{ minHeight: 420, display: "grid", placeItems: "center" }}><CircularProgress /></Box>;
  if (error && !data) return <Alert severity="error">{error}</Alert>;
  if (!data) return <Alert severity="error">Dashboard data is unavailable</Alert>;

  return (
    <Box sx={{ pb: 4 }}>
      <Box sx={{ mb: 2.5 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, color: "#0f172a" }}>Contact Dashboard</Typography>
        {/* <Typography variant="body2" color="text.secondary">Contact analytics with graph/table drill-down.</Typography> */}
      </Box>

      <Paper elevation={0} sx={{ p: 1.5, mb: 2, border: "1px solid #dbe7e5", borderRadius: 2 }}>
        <Grid container spacing={1.25} sx={{ alignItems: "center" }}>
          <Grid size={{ xs: 12, sm: 4, md: 3 }}>
            <DateRangeField
              label="From Date"
              value={draftRange.fromDate}
              max={today}
              onChange={(next) => {
               
                if (!next || next > today) return;

                setDraftRange((current) => ({
                  ...current,
                  fromDate: next,
                  toDate: "",
                }));
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4, md: 3 }}>
            <DateRangeField
              label="To Date"
              value={draftRange.toDate}
              min={draftRange.fromDate || undefined}
              max={today}
              onChange={(next) => {
                if (!draftRange.fromDate || !next) return;
                if (next > today || next < draftRange.fromDate) return;
                setDraftRange((current) => ({ ...current, toDate: next }));
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4, md: 6 }}>
            <Box sx={{ display: "flex", gap: 1 }}>
              <Button variant="contained" startIcon={<SearchOutlinedIcon />} onClick={handleSearch} disabled={loading} sx={{ bgcolor: "#0f766e", "&:hover": { bgcolor: "#0b5f59" } }}>Search</Button>
              <Button variant="outlined" startIcon={<RestartAltOutlinedIcon />} onClick={handleReset}>Reset</Button>
              {loading && <CircularProgress size={24} sx={{ ml: 1, alignSelf: "center" }} />}
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={1.5} sx={{ mb: 2 }}>
        {stats.map((item) => (
          <Grid key={item.label} size={{ xs: 12, sm: 6, lg: 3 }}>
            <Card
              elevation={0}
              onClick={() => item.filters !== null && Number(item.value) > 0 && handleOpenDetails({ title: item.label, filters: item.filters, allTime: true })}
              sx={{
                height: "100%",
                border: `1px solid ${item.color}33`,
                borderLeft: `4px solid ${item.color}`,
                borderRadius: 2,
                background: `linear-gradient(135deg, #fff 0%, ${item.lightColor} 100%)`,
                boxShadow: "0 4px 14px rgba(15,23,42,.05)",
                transition: "transform .18s ease, box-shadow .18s ease",
                cursor: item.filters !== null && Number(item.value) > 0 ? "pointer" : "default",

                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 7px 18px rgba(15,23,42,.10)",
                },
              }}
            >
              <CardContent
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1.4,
                  p: 1.6,

                  "&:last-child": {
                    pb: 1.6,
                  },
                }}
              >
                <Box
                  sx={{
                    width: 44,
                    height: 44,
                    flexShrink: 0,
                    borderRadius: 2,
                    bgcolor: item.color,
                    color: "#fff",
                    display: "grid",
                    placeItems: "center",
                    boxShadow: `0 5px 12px ${item.color}35`,
                  }}
                >
                  {item.icon}
                </Box>

                <Box sx={{ minWidth: 0 }}>
                  <Typography
                    sx={{
                      fontSize: 12.5,
                      fontWeight: 700,
                      color: item.darkColor,
                    }}
                  >
                    {item.label}
                  </Typography>

                  <Typography
                    sx={{
                      mt: 0.2,
                      fontSize: 25,
                      lineHeight: 1.15,
                      fontWeight: 800,
                      color: item.darkColor,
                    }}
                  >
                    {item.value}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}><AmChartsPanel data={data} onOpenDetails={handleOpenDetails} /></Grid>
        <Grid size={{ xs: 12 }}><ReactChartPanel data={data} onOpenDetails={handleOpenDetails} /></Grid>
      </Grid>

      {detailSelection && <DetailTableDialog selection={detailSelection} dateRange={appliedRange} onClose={() => setDetailSelection(null)} />}
    </Box>
  );
}

export default Dashboard;
