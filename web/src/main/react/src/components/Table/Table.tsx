import { CSSProperties, ReactElement, useCallback, useEffect, useMemo, useRef, useState } from "react"
import {
  Column,
  ColumnDef,
  ColumnPinningState,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  PaginationState,
  RowSelectionState,
  SortingState,
  Updater,
  useReactTable,
  VisibilityState,
} from "@tanstack/react-table"
import { Center, Group, Loader, Menu, Select, Stack, Table as MantineTable, Text, Tooltip } from "@mantine/core"
import classes from "./Table.module.css"
import { AppSpacing } from "global/types"
import { usePagination } from "@mantine/hooks"
import { ActionIcon } from "../ActionIcon/ActionIcon"
import { IconArrowDown, IconArrowUp, IconCheck, IconX } from "@tabler/icons-react"

export type TablePaginationProps = {
  state: PaginationState
  setPaginationState: (updater: Updater<PaginationState>) => void
  fullResultCount?: number
  manualPagination: boolean
  nextPage?: number
  showPageSizeControls?: boolean
  pageSizes?: string[]
}

export type TableSortingProps = {
  state: SortingState
  setSortingState: (updater: Updater<SortingState>) => void
  manualSorting: boolean
  enableMultiSort?: boolean
}

export type TableSelectionProps<T> = {
  state: RowSelectionState
  setRowSelectionState: (updater: Updater<RowSelectionState>) => void
  selectionColumn: ColumnDef<T>
  enableMultiRowSelection?: boolean
  enableClickToSelect?: boolean
  getRowId?: (row: T) => string
}

export type TableVisibilityProps = {
  state: VisibilityState
  setVisibilityState: (updater: Updater<VisibilityState>) => void
}

export type TablePinningProps = {
  state: ColumnPinningState
  setPinningState: (updater: Updater<ColumnPinningState>) => void
}

export type TableProps<T> = {
  // Standard Tanstack Table stuff
  columns: ColumnDef<T>[]
  data: T[]
  // Pagination is optional, but if passed in, it *must* include everything except fullResultCount
  pagination?: TablePaginationProps
  // Sorting is optional, but if passed in, it *must* include everything except manualSorting
  sorting?: TableSortingProps
  // Selection is optional, but if passed in, it *must* include everything except enableMultiRowSelection
  selection?: TableSelectionProps<T>
  // onRowClick is optional, to allow opening/redirection with context of the selected row
  onRowClick?: (id: T extends { id: infer U } ? U : never) => void
  // Visibility is optional, but if passed in, it *must* include everything
  visibility?: TableVisibilityProps
  // Pinning is optional, but if passed in, it *must* include everything
  pinning?: TablePinningProps
  // Text shown at the bottom of the table.
  caption?: string
  // whether the table is full-width
  fullWidth?: boolean
  // whether the table should allow resizing
  enableColumnResizing?: boolean
  // whether the table should automatically set column width based on the content
  autoColumnWidth?: boolean
  // A subset of Mantine's table props, to allow different visual styles
  verticalSpacing?: AppSpacing
  horizontalSpacing?: AppSpacing
  stickyHeader?: boolean
  isFetching?: boolean
}

/**
 * A generic table component that uses the Mantine Table component with Tanstack Table. The table requires a type T of data to display.
 *
 * Props are a mixture of Mantine props and Tanstack Table props.
 *
 * @param columns - The columns to display in the table. Must be ColumnDef<T>.
 * @param data - The data to display in the table. Must be of type T[]. If data is empty, the table will not render.
 * @param pagination - The pagination state and updater.
 * @param sorting - The sorting state and updater. Sorting can be automatic or manual, so ensure the manualSorting prop is set accordingly.
 * @param selection - The selection state and updater. If setting selection, ensure a column to allow selection is included in the columns prop.
 * @param onRowClick - A function to call when a row is clicked. The function should take the ID of the item T as an argument.
 * @param visibility - The visibility state and updater. This allows columns to be hidden or shown.
 * @param pinning - The pinning state and updater. This allows columns to be pinned to the left or right.
 * @param caption - The text to display at the bottom of the table
 * @param fullWidth - Whether the table should take up all available horizontal space. Defaults to true.
 * @param enableColumnResizing - Whether the table should allow resizing of columns. Defaults to false.
 * @param autoColumnWidth - Whether the table should automatically set column width based on the content. Defaults to false.
 * @param verticalSpacing - The vertical spacing between rows (MantineSpacing). Defaults to "md".
 * @param horizontalSpacing - The horizontal spacing between columns (MantineSpacing). Defaults to "md".
 * @param stickyHeader - Whether the header should be sticky (boolean). Defaults to false.
 * @param isFetching - Whether data is currently being fetched to determine if loading overlay should display
 */
export const Table = <T,>({
  columns,
  data,
  pagination,
  sorting,
  selection,
  onRowClick,
  visibility,
  pinning,
  caption,
  fullWidth = true,
  enableColumnResizing = false,
  autoColumnWidth = false,
  verticalSpacing = "sm",
  horizontalSpacing = "sm",
  stickyHeader = false,
  isFetching = false,
}: TableProps<T>): ReactElement => {
  // Store the column widths for resizing
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({})
  // Keep track of whether we are on the initial render (see useEffect below)
  const [initialRender, setInitialRender] = useState(true)

  // Time to set up tanstack table...
  const table = useReactTable<T>({
    defaultColumn: { size: 60, minSize: 60 },
    // if selection props were passed in, add the mandatory selection column as the first column. This MUST be the case as we slice off a column later on.
    columns: selection ? [selection.selectionColumn, ...columns] : columns,
    data,
    // always add the standard row models
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    // if pagination props were passed in, add the pagination row model
    getPaginationRowModel: pagination ? getPaginationRowModel() : undefined,
    state: {
      pagination: pagination?.state,
      sorting: sorting?.state,
      rowSelection: selection?.state,
      columnVisibility: visibility?.state,
      columnPinning: pinning?.state ?? { left: [], right: [] },
      columnSizing: columnWidths,
    },
    // pagination
    onPaginationChange: pagination?.setPaginationState,
    manualPagination: pagination?.manualPagination,
    // sorting
    onSortingChange: sorting?.setSortingState,
    manualSorting: sorting?.manualSorting, // enable this if, for example, we are performing sorting server-side
    enableMultiSort: sorting?.enableMultiSort, // whether we can sort on multiple columns (SHIFT + click)
    enableMultiRemove: sorting?.enableMultiSort, // whether we can remove multiple sorts by clicking a new column
    // visibility
    onColumnVisibilityChange: visibility?.setVisibilityState,
    // selection
    onRowSelectionChange: selection?.setRowSelectionState,
    enableRowSelection: selection !== undefined,
    enableMultiRowSelection: selection?.enableMultiRowSelection,
    getRowId: selection?.getRowId,
    // resizing
    enableColumnResizing: enableColumnResizing,
    columnResizeMode: "onEnd",
    onColumnSizingChange: setColumnWidths,
    // pinning
    onColumnPinningChange: pinning?.setPinningState,
  })

  const tableRef = useRef<HTMLTableElement>(null)

  /**
   * Calculate the column widths on the initial render, if autoColumnWidth is set.
   * This is done by measuring the width of each header cell in the table, and setting values in columnWidths.
   * The columns are always set to `width: max-content` on the initial render, and then set to the correct width after the first render.
   * This ensures that column resizing works correctly, as each column knows its correct width.
   */
  useEffect(() => {
    const columnsWithoutWidths = table
      .getAllColumns()
      .filter((col) => {
        const visibilityState = visibility?.state ?? {}
        const visible = visibilityState[col.id] ?? true
        const hasNoWidthEntry = columnWidths[col.id] === undefined

        return visible && hasNoWidthEntry
      })
      .map((col) => col.id)

    if (autoColumnWidth && (initialRender || columnsWithoutWidths.length > 0)) {
      // create an object to store the widths of each column as we find them
      const widths: Record<string, number> = {}
      // grab the table from the ref
      const tableElement = tableRef.current
      if (tableElement) {
        // get all the header cells
        const headers = tableElement.querySelectorAll("th")
        headers.forEach((header) => {
          const clientWidth = header.offsetWidth
          // name is stored as a data attribute on the header
          const columnName = header.getAttribute("data-name")
          if (columnsWithoutWidths.includes(columnName!)) {
            // set into our internal object
            widths[columnName!] = clientWidth
          }
        })
        // set the column widths on our component state
        setColumnWidths((current) => ({ ...current, ...widths }))
      }
      console.debug("Calculated table widths", widths, columnWidths)
    }
    // now we finished, any subsequent renders will not force the columns to fit all of the content
    setInitialRender(false)

    return () => setInitialRender(true)
  }, [autoColumnWidth, initialRender, visibility?.state])

  // Every cell has some padding on either side, and we need to compensate for this when setting the width of the internal divs.
  // This is quality code and there's no way you can convince me otherwise.
  // prettier-ignore
  const cellPadding = (spacing: AppSpacing) =>
    spacing === "xs" ? 8  :
    spacing === "sm" ? 16 :
    spacing === "md" ? 32 :
    spacing === "lg" ? 64 :
    spacing === "xl" ? 128 : 256

  /**
   * Method to set the styles on each table cell. Takes care of pinning and also sets the width of the cell, which is different on first render.
   */
  const getCellStyles = useCallback(
    (column: Column<T>): CSSProperties => {
      const isPinned = column.getIsPinned()
      const isLastLeftPinnedColumn = isPinned === "left" && column.getIsLastColumn("left")
      const isFirstRightPinnedColumn = isPinned === "right" && column.getIsFirstColumn("right")

      const verticalPadding = cellPadding(verticalSpacing)
      const horizontalPadding = cellPadding(horizontalSpacing)

      const columnWidth = columnWidths[column.id]
      let widths = {}
      if (columnWidth || !autoColumnWidth) {
        let widthWithPadding = (columnWidth ?? column.columnDef.size) + horizontalPadding
        widthWithPadding = column.columnDef.maxSize ? Math.min(widthWithPadding, column.columnDef.maxSize) : widthWithPadding
        widthWithPadding = column.columnDef.minSize ? Math.max(widthWithPadding, column.columnDef.minSize) : widthWithPadding
        widths = {
          minWidth: widthWithPadding,
          maxWidth: widthWithPadding,
        }
      }

      const pinnedStart = column.getStart("left")

      return {
        boxShadow: isLastLeftPinnedColumn ? "-4px 0 4px -4px gray inset" : isFirstRightPinnedColumn ? "4px 0 4px -4px gray inset" : undefined,
        left: isPinned === "left" ? `${pinnedStart - 1}px` : undefined,
        right: isPinned === "right" ? `${column.getAfter("right") - 1}px` : undefined,
        position: isPinned ? "sticky" : "relative",
        zIndex: isPinned ? 1 : 0,
        width: "max-content",
        cursor: onRowClick ? "pointer" : "default",
        padding: `${verticalPadding}px ${horizontalPadding}px`,
        ...widths,
      }
    },
    [columnWidths],
  )

  /**
   * Method to set the styles on the cell's internal div, which needs an explicit size set so the ellipsis works.
   */
  const getInternalDivStyles = useCallback((column: Column<T>, initialRender: boolean): CSSProperties => {
    return {
      width: initialRender ? "max-content" : "auto",
    }
  }, [])

  // This stuff is useful for debugging any attempt at memoization, so leaving it here for now.
  // const theColumns = table
  //   .getHeaderGroups()
  //   .map((headerGroup) => headerGroup.headers)
  //   .map((headers) => headers.map((header) => header.column))
  //
  // const theFirstRow = table
  //   .getRowModel()
  //   .rows[0].getVisibleCells()
  //   .map((cell) => cell.column)
  //
  // for (let i = 0; i < theColumns.length; i++) {
  //   // compare header with cell below
  //   for (let j = 0; j < theColumns[i].length; j++) {
  //     console.log("theColumns[i, j]", theColumns[i][j])
  //     const column = theColumns[i][j]
  //     const cell = theFirstRow[j]
  //     console.log("column", column)
  //     console.log("cell", cell)
  //     console.log(Object.is(column, cell))
  //   }
  // }

  return (
    <Stack w={fullWidth ? "100%" : undefined} gap={"md"} maw={"100%"}>
      <MantineTable.ScrollContainer minWidth={"100%"} type={"native"}>
        <MantineTable
          ref={tableRef}
          stickyHeader={stickyHeader}
          withColumnBorders={true}
          withRowBorders={true}
          withTableBorder={true}
          highlightOnHover
          highlightOnHoverColor={"gray.0"}
          captionSide={"bottom"}
          classNames={classes}>
          {caption && <MantineTable.Caption pos={"sticky"}>{caption}</MantineTable.Caption>}
          {/* header, including sorting functionality */}
          <MantineTable.Thead>
            {table.getHeaderGroups().map((headerGroup, index) => (
              <MantineTable.Tr key={`${headerGroup.id}-${index}`}>
                {headerGroup.headers.map((header, index) => {
                  const { column, id } = header
                  const headerCellStyles = getCellStyles(column)
                  const internalDivStyles = getInternalDivStyles(column, initialRender)
                  return (
                    <MantineTable.Th key={`${id}-${index}`} colSpan={header.colSpan} style={headerCellStyles} data-name={id}>
                      <>
                        {/* If sorting has been passed in, we render a sortable column */}
                        {sorting ? (
                          <Group wrap={"nowrap"}>
                            <div onClick={header.column.getToggleSortingHandler()} style={internalDivStyles}>
                              {flexRender(header.column.columnDef.header, header.getContext())}
                            </div>
                            {{
                              asc: <IconArrowUp />,
                              desc: <IconArrowDown />,
                            }[header.column.getIsSorted() as string] ?? null}
                          </Group>
                        ) : (
                          // Otherwise, we just render the header
                          <div style={internalDivStyles}>{flexRender(header.column.columnDef.header, header.getContext())}</div>
                        )}
                        {enableColumnResizing && (
                          <div
                            className={classes.resizer}
                            data-resizing={header.column.getIsResizing()}
                            onMouseDown={header.getResizeHandler()}
                            onTouchStart={header.getResizeHandler()}
                            onDoubleClick={() => header.column.resetSize()}
                            style={{
                              transform: header.column.getIsResizing() ? `translateX(${table.getState().columnSizingInfo.deltaOffset ?? 0}px)` : "",
                            }}
                          />
                        )}
                      </>
                    </MantineTable.Th>
                  )
                })}
              </MantineTable.Tr>
            ))}
          </MantineTable.Thead>
          <MantineTable.Tbody>
            {/* If we have nothing to show, just render a single row to say so */}
            {table.getRowCount() === 0 ? (
              <MantineTable.Tr>
                <MantineTable.Td colSpan={table.getAllColumns().length}>
                  <Center>No data available</Center>
                </MantineTable.Td>
              </MantineTable.Tr>
            ) : (
              // Print the table data
              table.getRowModel().rows.map((row, index) => (
                <MantineTable.Tr
                  key={row.id ?? `row-${index}`}
                  onClick={
                    selection?.enableClickToSelect
                      ? row.getToggleSelectedHandler()
                      : onRowClick
                        ? () => {
                            if (typeof row.original === "object" && row.original !== null && "id" in row.original) {
                              onRowClick?.(row.original.id as T extends { id: infer U } ? U : never)
                            }
                          }
                        : undefined
                  }
                  data-selected={selection && row.getIsSelected()}>
                  {row.getVisibleCells().map((cell, index) => {
                    const { column, id } = cell
                    const contentCellStyles = getCellStyles(column)
                    const internalDivStyles = getInternalDivStyles(column, initialRender)
                    return (
                      /* On the initial render, we want to set max-content on the content as well, and after that we just render the exact size */
                      <MantineTable.Td key={`${id}-${index}`} style={contentCellStyles}>
                        <div style={internalDivStyles}>{flexRender(column.columnDef.cell, cell.getContext())}</div>
                      </MantineTable.Td>
                    )
                  })}
                </MantineTable.Tr>
              ))
            )}
          </MantineTable.Tbody>
        </MantineTable>
      </MantineTable.ScrollContainer>

      <Group justify={"flex-end"}>
        {pagination && (
          <PaginationTableControls
            pageSize={table.getState().pagination.pageSize}
            setPageSize={table.setPageSize}
            pageIndex={table.getState().pagination.pageIndex}
            setPageIndex={table.setPageIndex}
            fullResultCount={pagination?.fullResultCount}
            nextPage={pagination?.nextPage}
            isFetching={isFetching}
            showPageSizeControls={pagination?.showPageSizeControls !== undefined ? pagination?.showPageSizeControls : true}
            pageSizes={pagination.pageSizes ?? ["10", "25", "50", "100"]}
          />
        )}
        {/* If visibility was set, show the column visibility button, slicing off the first column if we have a selection column */}
        {visibility && <ColumnVisibility columns={selection ? table.getAllLeafColumns().slice(1) : table.getAllLeafColumns()} />}
      </Group>
    </Stack>
  )
}

type TableControlsProps = {
  pageSize: number
  setPageSize: (updater: Updater<number>) => void
  pageIndex: number
  setPageIndex: (index: number) => void
  fullResultCount?: number | undefined
  nextPage?: number | undefined
  isFetching?: boolean
  showPageSizeControls?: boolean
  pageSizes: string[]
}

/**
 * A component that displays the pagination controls for a Table component.
 *
 * Is rendered when the pagination prop is passed to the Table component.
 *
 * @param pageSize - The number of items per page
 * @param setPageSize - The function to set the number of items per page
 * @param pageIndex - The current page index
 * @param setPageIndex - The function to set the page index
 * @param fullResultCount - The total number of results. If omitted, the total will not be shown & last page button will be disabled
 * @param nextPage - The nextPage value from the metadata of an API response. Use in place of fullResultCount if the total number of pages/results is unknown.
 * @param isFetching - Whether data is currently being fetched to determine if loading overlay should display
 * @param showPageSizeControls - Whether the pageSize dropdown should render or not. Defaults to true.
 * @param pageSizes - The options for the page size dropdown
 */

export const PaginationTableControls = ({
  pageSize,
  setPageSize,
  pageIndex,
  setPageIndex,
  fullResultCount,
  nextPage,
  isFetching,
  showPageSizeControls,
  pageSizes,
}: TableControlsProps) => {
  // Pagination controls
  const currentPage = pageIndex + 1

  const pageCount: number = useMemo(() => {
    return fullResultCount ? Math.ceil(fullResultCount / pageSize) : nextPage ? nextPage : currentPage
  }, [fullResultCount, pageSize, nextPage, currentPage])

  const startItem = pageIndex * pageSize + 1
  const firstPage = pageIndex === 0
  const lastPage = pageIndex === pageCount - 1

  const selectValues = Array.from({ length: pageCount }, (_, i) => `Page ${i + 1}`)

  const pagination = usePagination({
    total: pageCount,
    page: currentPage,
    onChange: (page: number) => {
      setPageIndex(page - 1)
    },
  })

  const pageSizeOptions = useMemo<string[]>(() => {
    return Array.from(new Set([...pageSizes, pageSize.toString()]))
  }, [pageSizes, pageSize])

  return (
    <Group gap={"sm"} justify={"space-between"} style={{ flexGrow: 1 }}>
      {/*Pagination Controls*/}
      {pageCount >= 1 && (
        <Group justify={"flex-start"} gap={"xs"}>
          <ActionIcon
            matchFormSize
            icon={"arrow-left-to-line"}
            size={"xs"}
            variant={"outline"}
            onClick={pagination.first}
            aria-label={"Go to first page"}
            disabled={firstPage}
          />
          <ActionIcon
            matchFormSize
            icon={"arrow-left"}
            size={"xs"}
            variant={"outline"}
            onClick={pagination.previous}
            aria-label={"Go to previous page"}
            disabled={firstPage}
          />

          {fullResultCount && (
            <Select
              rightSectionPointerEvents="none"
              variant={"outline"}
              size={"xs"}
              value={`Page ${currentPage}`}
              onChange={(pageString) => pageString && setPageIndex(Number(pageString.slice(5)) - 1)}
              data={selectValues}
              width={100}
              aria-label={"Select page"}
              disabled={firstPage && lastPage}
            />
          )}

          <ActionIcon
            matchFormSize
            icon={"arrow-right"}
            size={"xs"}
            variant={"outline"}
            onClick={pagination.next}
            aria-label={"Go to next page"}
            disabled={lastPage}
          />

          {fullResultCount && (
            <ActionIcon
              matchFormSize
              icon={"arrow-right-to-line"}
              size={"xs"}
              variant={"outline"}
              onClick={pagination.last}
              aria-label={"Go to last page"}
              disabled={lastPage}
            />
          )}

          {!fullResultCount && !isFetching && <Text fz={14} fw={700}>{`Page ${currentPage}`}</Text>}
          {isFetching && <Loader size={"xs"} />}
        </Group>
      )}

      {fullResultCount && (
        <Text>
          Showing{" "}
          <Text span fw={700}>
            {`${startItem}-${Math.min(startItem + pageSize - 1, fullResultCount)} of ${fullResultCount}`}
          </Text>{" "}
          results
        </Text>
      )}
      {/*Page Size controls*/}
      {showPageSizeControls && (
        <Tooltip label={"Results per page"}>
          <Select
            className={classes.pageSizeSelect}
            rightSectionPointerEvents="none"
            size={"xs"}
            value={pageSize.toString()}
            data={pageSizeOptions}
            aria-label={"Select page size"}
            onChange={(newPageSize) => setPageSize(Number(newPageSize))}
            disabled={isFetching}
            allowDeselect={false}
          />
        </Tooltip>
      )}
    </Group>
  )
}

type ColumnVisibilityProps<T> = {
  columns: Column<T>[] | undefined
}

/**
 * A component that displays a menu to show or hide columns in a GenericTableMantine component.
 *
 * Shows all columns in a dropdown menu, with a checkmark or cross to indicate visibility. Columns that do not have visibility enabled are disabled.
 *
 * @param columns All columns in the table. Should be called with getAllLeafColumns() from the useReactTable hook.
 */
export const ColumnVisibility = <T,>({ columns }: ColumnVisibilityProps<T>) => (
  <Group>
    {/* Oh Menu, why do you refuse to serve me well? You'll get your just desserts. */}
    <Menu id={"column-visibility-menu"} closeOnItemClick={false} position={"bottom-start"}>
      <Menu.Target>
        <Tooltip label={"Show / Hide Columns"}>
          <ActionIcon icon={"bars-filter"} variant={"subtle"} />
        </Tooltip>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>Columns</Menu.Label>
        <Menu.Divider />
        {columns?.map((column) => (
          <Menu.Item
            disabled={!column.getCanHide()}
            key={column.id + "-toggle"}
            leftSection={column.getIsVisible() ? <IconCheck /> : <IconX />}
            onClick={column.getToggleVisibilityHandler()}>
            {column.columnDef.header?.toString()}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  </Group>
)
