import axios from '../../../../api/axios'

export type ReactTableColumnConfig = {
  key: string
  header: string
  visible?: boolean
  order?: number
  size?: number
  sortable?: boolean
  renderer?: string
}

const normalizeTableConfig = (raw: unknown): ReactTableColumnConfig[] => {
  const value = raw as any

  const rows = Array.isArray(value)
    ? value
    : Array.isArray(value?.columns)
      ? value.columns
      : Array.isArray(value?.content)
        ? value.content
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
        false,
      renderer:
        String(item?.renderer ?? '').trim() || undefined,
    }))
    .filter(
      (item: ReactTableColumnConfig) =>
        item.key &&
        item.header &&
        item.visible !== false,
    )
    .sort(
      (first: ReactTableColumnConfig, second: ReactTableColumnConfig) =>
        Number(first.order ?? 0) - Number(second.order ?? 0),
    )
}

const tableConfigCache = new Map<string, ReactTableColumnConfig[]>()
const tableConfigPromises = new Map<string, Promise<ReactTableColumnConfig[]>>()

export const getReactTableConfig = async (
  table: string,
): Promise<ReactTableColumnConfig[]> => {
  const key = table.trim().toUpperCase()
  const cached = tableConfigCache.get(key)
  if (cached) return cached

  const inFlight = tableConfigPromises.get(key)
  if (inFlight) return inFlight

  const request = axios
    .get(`/contact/table-config/${encodeURIComponent(key)}`)
    .then((response) => {
      const data = response.data

      if (String(data?.status || '').toUpperCase() !== 'SUCCESS') {
        throw new Error(
          data?.error ||
            data?.message ||
            `Unable to load ${key} table config`,
        )
      }

      const config = normalizeTableConfig(data?.data)
      tableConfigCache.set(key, config)
      return config
    })
    .finally(() => {
      tableConfigPromises.delete(key)
    })

  tableConfigPromises.set(key, request)
  return request
}


export type DashboardTableConfigMap = Record<string, ReactTableColumnConfig[]>

let dashboardTableConfigPromise: Promise<DashboardTableConfigMap> | null = null

export const getDashboardTableConfigs = async (): Promise<DashboardTableConfigMap> => {
  if (!dashboardTableConfigPromise) {
    dashboardTableConfigPromise = axios
      .get('/contact/table-config/dashboard/all')
      .then((response) => {
        const data = response.data

        if (String(data?.status || '').toUpperCase() !== 'SUCCESS') {
          throw new Error(
            data?.error ||
              data?.message ||
              'Unable to load dashboard table configs',
          )
        }

        const raw = data?.data ?? {}

        return Object.fromEntries(
          Object.entries(raw).map(([key, value]) => [
            key,
            normalizeTableConfig(value),
          ]),
        )
      })
      .catch((error) => {
        dashboardTableConfigPromise = null
        throw error
      })
  }

  return dashboardTableConfigPromise
}

