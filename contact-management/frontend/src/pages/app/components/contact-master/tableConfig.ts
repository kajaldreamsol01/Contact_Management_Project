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

export const getReactTableConfig = async (
  table: string,
): Promise<ReactTableColumnConfig[]> => {
  const response = await axios.get(
    `/contact/table-config/${encodeURIComponent(table)}`,
  )

  const data = response.data

  if (String(data?.status || '').toUpperCase() !== 'SUCCESS') {
    throw new Error(
      data?.error ||
        data?.message ||
        `Unable to load ${table} table config`,
    )
  }

  return normalizeTableConfig(data?.data)
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

