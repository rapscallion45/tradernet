import React, { FC, useState } from "react"
import { Meta, StoryContext, StoryFn, StoryObj } from "@storybook/react"
import {
  ColumnDef,
  ColumnPinningState,
  createColumnHelper,
  GroupColumnDef,
  PaginationState,
  RowSelectionState,
  SortingState,
  Updater,
  VisibilityState,
} from "@tanstack/react-table"
import { Checkbox, Flex } from "@mantine/core"
import { expect, userEvent, within } from "@storybook/test"
import { allPeople, Person, somePeople, spacingControl, unknownPeople } from "../../../storybook/mixins"
import { Table, TableProps } from "./Table"

// region Column Setup

/**
 * Column Setup - WARNING! Changing the column setup may break the stories, due to the deterministic nature of the data generation.
 */

// Why is this the helper if it creates types that literally break the type system? grrr.
const columnHelper = createColumnHelper<Person>()

const allColumns: ColumnDef<Person>[] = [
  columnHelper.accessor("id", { header: "ID", size: 50 }),
  columnHelper.accessor("name", { header: "Name", size: 150 }),
  columnHelper.accessor("dob", { header: "Date of Birth", cell: (ctx) => ctx.getValue().toUTCString(), size: 200 }),
  columnHelper.accessor("country", { header: "Country", size: 150 }),
  columnHelper.accessor("favouriteNumber", { header: "Favourite Number", size: 100 }),
  columnHelper.accessor("email", { header: "Email" }),
  columnHelper.accessor("bio", { header: "Bio", size: 250 }),
] as ColumnDef<Person>[] // bit of a hack to make TypeScript Happy: https://github.com/TanStack/table/issues/4302

const someColumns = allColumns.slice(0, 3)

const nestedColumns: GroupColumnDef<Person>[] = [
  columnHelper.group({
    header: "Contact Info",
    columns: [allColumns[1], allColumns[2], allColumns[3], allColumns[5]],
  }),
  columnHelper.group({
    header: "Other Info",
    columns: [allColumns[0], allColumns[4], allColumns[6]],
  }),
] as GroupColumnDef<Person>[]

// endregion

// region Storybook Meta

const meta = {
  title: "Tradernet/Table",
  component: Table,
  argTypes: {
    columns: {
      control: "select",
      description: "The columns to display in the table",
      options: ["All Columns", "Some Columns", "No Columns"],
      mapping: {
        "All Columns": allColumns,
        "Some Columns": someColumns,
        "No Columns": [],
      },
      table: {
        type: { summary: "ColumnDef<T>[]", detail: "An array of Tanstack Table ColumnDef<T> objects" },
      },
    },
    data: {
      control: "select",
      description: "The data to display in the table",
      options: ["All Data", "Some Data", "No Data"],
      mapping: {
        "All Data": allPeople,
        "Some Data": somePeople,
        "No Data": [],
      },
      table: {
        type: { summary: "T[]", detail: "An array of type T, which must match T in columns" },
      },
    },
    pagination: {
      control: false,
      description: "The pagination configuration for the table. See the sub-items below for detail about each element.",
      table: {
        type: {
          summary: "TablePaginationProps",
          detail:
            "state: PaginationState\n" +
            "setPaginationState: (updater: Updater<PaginationState>) => void\n" +
            "fullResultCount?: number\n" +
            "manualPagination: boolean",
        },
        category: "State",
      },
    },
    sorting: {
      control: false,
      description: "The sorting configuration for the table. See the sub-items below for detail about each element.",
      table: {
        type: {
          summary: "TableSortingProps",
          detail:
            "state: SortingState\n" + "setSortingState: (updater: Updater<SortingState>) => void\n" + "manualSorting: boolean\n" + "enableMultiSort?: boolean",
        },
        category: "State",
      },
    },
    selection: {
      control: false,
      description: "The selection configuration for the table. See the sub-items below for detail about each element.",
      table: {
        type: {
          summary: "TableSelectionProps<T>",
          detail:
            "state: RowSelectionState\n" +
            "setRowSelectionState: (updater: Updater<RowSelectionState>) => void\n" +
            "selectionColumn: ColumnDef<T>\n" +
            "enableRowSelection: true\n" +
            "enableMultiRowSelection?: boolean",
        },
        category: "State",
      },
    },
    visibility: {
      control: false,
      description: "The pagination configuration for the table. See the sub-items below for detail about each element.",
      table: {
        type: {
          summary: "TableVisibilityProps",
          detail: "state: VisibilityState\n" + "setVisibilityState: (updater: Updater<VisibilityState>) => void",
        },
        category: "State",
      },
    },
    caption: {
      control: "text",
      description: "The caption to display at the bottom of the table",
      table: { category: "Layout" },
    },
    stickyHeader: {
      control: "boolean",
      description: "Whether to make the header sticky",
      table: { category: "Layout" },
    },
    fullWidth: {
      control: "boolean",
      description: "Whether the table should be full width. In this mode the table will take up the full width of its container, and scroll horizontally.",
      table: { category: "Layout" },
    },
    verticalSpacing: {
      description: "The vertical spacing between elements in the table",
      table: spacingControl.table,
    },
    horizontalSpacing: {
      description: "The horizontal spacing between elements in the table",
      table: spacingControl.table,
    },
  },
  decorators: [
    (Story: StoryFn, context: StoryContext) => {
      return context.args?.fullWidth ? (
        <Story />
      ) : (
        <Flex h={600} w={1000}>
          <Story />
        </Flex>
      )
    },
  ],
} satisfies Meta<typeof Table<Person>>

export default meta

type Story = StoryObj<typeof Table<Person>>

// endregion

export const Basic: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
  },
  play: ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check that 100 rows are rendered
    const rows = canvas.getAllByRole("row")
    // includes the header row
    expect(rows).toHaveLength(101)
  },
}

export const NoColumnsOrData: Story = {
  args: {
    columns: [],
    data: [],
  },
  play: ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check that 100 rows are rendered
    const rows = canvas.getAllByRole("row")
    // includes the header row
    expect(rows).toHaveLength(2)
    // check that the no data message is displayed
    const noData = canvas.getByText("No data available")
    expect(noData).toBeInTheDocument()
  },
}

export const ColumnsWithNoData: Story = {
  args: {
    columns: allColumns,
    data: [],
  },
  play: ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check that 100 rows are rendered
    const rows = canvas.getAllByRole("row")
    // includes the header row
    expect(rows).toHaveLength(2)
    // check that the no data message is displayed
    expect(canvas.getByText("No data available")).toBeInTheDocument()
    // check for "Name" and "Date of Birth" columns
    expect(canvas.getByText("Name")).toBeInTheDocument()
    expect(canvas.getByText("Date of Birth")).toBeInTheDocument()
  },
}

// local state management, that we pass into the table to use
type SortingProps = Pick<TableProps<Person>, "sorting">

const useSortingState: (initialState: SortingState) => SortingProps = (initialState: SortingState) => {
  const [state, setState] = useState(initialState)
  const setSortingState = (updater: Updater<SortingState>) => {
    const newSorting = typeof updater === "function" ? updater(state ?? []) : updater
    console.debug("Setting table sorting from", state, "to", newSorting)
    setState(newSorting)
  }
  return { sorting: { state, setSortingState, enableMultiSort: true, manualSorting: false } }
}

// Helper component to wrap the GenericTableMantine component with sorting state
const TableWithSorting: FC<TableProps<Person>> = (args) => {
  const { sorting } = useSortingState([{ id: "dob", desc: true }])
  return <Table<Person> {...args} sorting={sorting} />
}

export const Sorting: Story = {
  render: (args) => <TableWithSorting {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // get all rows
    let rows = canvas.getAllByRole("row")
    // check that 100 rows are rendered (includes the header row)
    expect(rows).toHaveLength(101)
    // check for "Name" and "Date of Birth" columns
    const nameHeader = canvas.getByText("Name")
    expect(nameHeader).toBeInTheDocument()
    const dateOfBirthHeader = canvas.getByText("Date of Birth")
    expect(dateOfBirthHeader).toBeInTheDocument()
    // get first row and check it's Gina Walker
    expect(rows[1]).toHaveTextContent("Gina Walker")
    expect(rows[1]).toHaveTextContent("01 Dec 2004")
    // click the name header to sort by name
    await userEvent.click(nameHeader)
    // get first row and check it's now Agnes Herman
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Agnes Herman")
    expect(rows[1]).toHaveTextContent("23 Aug 1966")
    // click the name header again to sort by name descending
    await userEvent.click(nameHeader)
    // get first row and check it's now Zachary Ebert
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Zachary Ebert")
    expect(rows[1]).toHaveTextContent("15 Jul 1968")
    // reset to sort by date of birth
    await userEvent.click(dateOfBirthHeader)
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Gina Walker")
    expect(rows[1]).toHaveTextContent("01 Dec 2004")
  },
}

type PaginationProps = Pick<TableProps<Person>, "pagination">

const usePaginationStateAllData = (initialState: PaginationState, delaySeconds?: number) => {
  const [state, setState] = useState(initialState)
  const [isFetching, setIsFetching] = useState(false)

  const setPaginationState = (updater: Updater<PaginationState>) => {
    setIsFetching(true)
    setTimeout(
      () => {
        setState((prev) => {
          const newPagination = typeof updater === "function" ? updater(prev) : updater
          console.debug("Setting table pagination from", prev, "to", newPagination)
          return newPagination
        })
        setIsFetching(false)
      },
      (delaySeconds ?? 0) * 1000,
    )
  }
  return { pagination: { state, setPaginationState, fullResultCount: allPeople.length, manualPagination: false }, isFetching }
}

const TableWithPaginationAllData: FC<TableProps<Person>> = (args) => {
  const { pagination } = usePaginationStateAllData({ pageIndex: 0, pageSize: 10 })
  return <Table<Person> {...args} pagination={pagination} />
}

export const Pagination: Story = {
  render: (args) => <TableWithPaginationAllData {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // get all rows
    let rows = canvas.getAllByRole("row")
    // check that 100 rows are rendered (includes the header row)
    expect(rows).toHaveLength(11)
    // check for "Name" and "Date of Birth" columns
    const nameHeader = canvas.getByText("Name")
    expect(nameHeader).toBeInTheDocument()
    const dateOfBirthHeader = canvas.getByText("Date of Birth")
    expect(dateOfBirthHeader).toBeInTheDocument()
    // get first row and check it's Damon Keebler
    expect(rows[1]).toHaveTextContent("Damon Keebler")
    expect(rows[1]).toHaveTextContent("11 Jul 1985")
    // click the next page button
    const nextPageButton = canvas.getByRole("button", { name: "Go to next page" })
    await userEvent.click(nextPageButton)
    // get first row and check it's Vivian Swift
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Vivian Swift")
    expect(rows[1]).toHaveTextContent("09 Nov 1943")
    // click the last page button
    const lastPageButton = canvas.getByRole("button", { name: "Go to last page" })
    await userEvent.click(lastPageButton)
    // get first row and check it's Walter Bode
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Walter Bode")
    expect(rows[1]).toHaveTextContent("15 Apr 1995")
    // click the first page button
    const firstPageButton = canvas.getByRole("button", { name: "Go to first page" })
    await userEvent.click(firstPageButton)
    // get first row and check it's Damon Keebler
    rows = canvas.getAllByRole("row")
    expect(rows[1]).toHaveTextContent("Damon Keebler")
    expect(rows[1]).toHaveTextContent("11 Jul 1985")
    // select Page 5 from the dropdown
    // Annoyingly here, Mantine renders a Select as a textbox,  which then seems to pop up a combobox
    // in a portal. We might need to roll our own combobox if we want better control here.
    // const pageSelect = canvas.getByRole("textbox", { name: "Select page" })
    // await userEvent.click(pageSelect)
  },
}

const TableWithPaginationAllDataLoading: FC<TableProps<Person>> = (args) => {
  const { pagination, isFetching } = usePaginationStateAllData({ pageIndex: 0, pageSize: 10 }, 0.5)
  return <Table<Person> {...args} isFetching={isFetching} pagination={pagination} />
}

export const PaginationWithLoading: Story = {
  render: (args) => <TableWithPaginationAllDataLoading {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
}

// A bit of duplication here but lesser of two evils
const usePaginationStateSomeData: (initialState: PaginationState) => PaginationProps = (initialState: PaginationState) => {
  const [state, setState] = useState(initialState)
  const setPaginationState = (updater: Updater<PaginationState>) => {
    const newPagination = typeof updater === "function" ? updater(state) : updater
    console.debug("Setting table pagination from", state, "to", newPagination)
    setState(newPagination)
  }
  return { pagination: { state, setPaginationState, fullResultCount: somePeople.length, manualPagination: false, pageSizes: ["5", "10", "15"] } }
}

const TableWithPaginationSomeData: FC<TableProps<Person>> = (args) => {
  const { pagination } = usePaginationStateSomeData({ pageIndex: 0, pageSize: 10 })
  return <Table<Person> {...args} pagination={pagination} />
}

export const PaginationSmallDataSet: Story = {
  render: (args) => <TableWithPaginationSomeData {...args} />,
  args: {
    columns: allColumns,
    data: somePeople,
  },
}

type UsePaginationReturn = {
  pagination: {
    state: PaginationState
    setPaginationState: (updater: Updater<PaginationState>) => void
    manualPagination: boolean
    nextPage: number | undefined
  }
  pageData: Person[]
  isFetching: boolean
}

// A bit more duplication, but lesser of three evils
// Here instead of returning fullResultCount within the pagination, we return current/nextPage, simulating not knowing how many pages/results there are and getting some metaData back from a response
const usePaginationStateUnknownData = (initialState: PaginationState, delaySeconds: number) => {
  const [state, setState] = useState(() => initialState)
  const [isFetching, setIsFetching] = useState(false)

  const setPaginationState = (updater: Updater<PaginationState>) => {
    setIsFetching(true)
    setTimeout(() => {
      setState((prev) => {
        const newPagination = typeof updater === "function" ? updater(prev) : updater
        console.debug("Setting table pagination from", prev, "to", newPagination)
        return newPagination
      })
      setIsFetching(false)
    }, delaySeconds * 1000)
  }

  const totalPages = Math.ceil(unknownPeople.length / state.pageSize)
  const currentPage = state.pageIndex + 1
  const nextPage = currentPage < totalPages ? currentPage + 1 : undefined

  return {
    pagination: { state, setPaginationState, manualPagination: false, nextPage },
    isFetching,
  }
}

const TableWithPaginationUnknownData: FC<TableProps<Person>> = (args) => {
  const { pagination, isFetching } = usePaginationStateUnknownData({ pageIndex: 0, pageSize: 10 }, 0.5)
  console.debug("pagination", pagination?.nextPage)

  return <Table<Person> {...args} isFetching={isFetching} pagination={pagination} data={unknownPeople} />
}

export const PaginationUnknownDataSet: Story = {
  render: (args) => <TableWithPaginationUnknownData {...args} />,
  args: {
    columns: allColumns,
    data: [],
  },
}

type SelectionProps = Pick<TableProps<Person>, "selection">

const selectionColumn: ColumnDef<Person> = {
  id: "select",
  header: ({ table }) => (
    <div style={{ textAlign: "center" }}>
      <Checkbox checked={table.getIsAllRowsSelected()} indeterminate={table.getIsSomeRowsSelected()} onChange={table.getToggleAllRowsSelectedHandler()} />
    </div>
  ),
  cell: ({ row }) => <Checkbox color={"grape.6"} checked={row.getIsSelected()} onChange={row.getToggleSelectedHandler()} />,
}

const useSelectionState: () => SelectionProps = () => {
  const [state, setState] = useState({})
  const setRowSelectionState = (updater: Updater<RowSelectionState>) => {
    const newRowSelection = typeof updater === "function" ? updater(state) : updater
    console.debug("Setting row selection from", state, "to", newRowSelection)
    setState(newRowSelection)
  }
  return {
    selection: {
      state,
      setRowSelectionState,
      selectionColumn,
      enableRowSelection: true,
      enableMultiRowSelection: true,
      enableClickToSelect: true,
      getRowId: (row: Person) => row.id,
    },
  }
}

const TableWithSelection: FC<TableProps<Person>> = (args) => {
  const { selection } = useSelectionState()
  return <Table<Person> {...args} selection={selection} />
}

export const Selection: Story = {
  render: (args) => <TableWithSelection {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
}

const TableWithOnClick: FC<TableProps<Person>> = (args) => {
  const onRowClick = (id: string) => {
    alert(`Row clicked: ${id}`)
  }
  return <Table<Person> {...args} onRowClick={onRowClick} />
}

export const onClick: Story = {
  render: (args) => <TableWithOnClick {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
}

type VisibilityProps = Pick<TableProps<Person>, "visibility">

function useVisibilityState(): VisibilityProps {
  const [state, setState] = useState<VisibilityState>({ email: false })
  const setVisibilityState = (updater: Updater<VisibilityState>) => {
    const newVisibility = typeof updater === "function" ? updater(state) : updater
    console.debug("Setting row visibility from", state, "to", newVisibility)
    setState(newVisibility)
  }
  return { visibility: { state, setVisibilityState } }
}

const TableWithVisibility: FC<TableProps<Person>> = (args) => {
  const { visibility } = useVisibilityState()
  const { pagination } = usePaginationStateSomeData({ pageIndex: 0, pageSize: 2 })

  return <Table<Person> {...args} visibility={visibility} pagination={pagination} />
}

export const Visibility: Story = {
  render: (args) => <TableWithVisibility {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
}

type PinningProps = Pick<TableProps<Person>, "pinning">

function usePinningState(): PinningProps {
  const [state, setState] = useState<ColumnPinningState>({ left: ["id"], right: [] })
  const setPinningState = (updater: Updater<ColumnPinningState>) => {
    const newPinning = typeof updater === "function" ? updater(state) : updater
    console.debug("Setting row pinning from", state, "to", newPinning)
    setState(newPinning)
  }
  return { pinning: { state, setPinningState } }
}

const TableWithPinning: FC<TableProps<Person>> = (args) => {
  const { pinning } = usePinningState()
  return <Table<Person> {...args} pinning={pinning} />
}

export const Pinning: Story = {
  render: (args) => <TableWithPinning {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
  },
}

export const StickyHeader: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    stickyHeader: true,
  },
}

export const FullWidth: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    fullWidth: true,
    horizontalSpacing: "sm",
    stickyHeader: true,
    autoColumnWidth: true,
  },
  decorators: [
    (Story: StoryFn) => (
      <Flex h={600} w={"100%"}>
        <Story />
      </Flex>
    ),
  ],
}
export const NotFullWidth: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    fullWidth: false,
  },
}

export const CustomSpacing: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    verticalSpacing: "xs",
    horizontalSpacing: "xl",
    caption: "This is a custom caption",
  },
}

const TableWithEverything: FC<TableProps<Person>> = (args) => {
  const { sorting } = useSortingState([{ id: "age", desc: true }])
  const { pagination } = usePaginationStateAllData({ pageIndex: 0, pageSize: 5 })
  const { selection } = useSelectionState()
  const { visibility } = useVisibilityState()
  const { pinning } = usePinningState()
  const caption = selection?.state ? `${Object.keys(selection.state).length} rows selected` : "No rows selected"
  const onRowClick = (id: string) => {
    alert(`Row clicked: ${id}`)
  }
  return (
    <Table<Person>
      {...args}
      sorting={sorting}
      pagination={pagination}
      selection={selection}
      visibility={visibility}
      pinning={pinning}
      caption={caption}
      onRowClick={onRowClick}
    />
  )
}

export const KitchenSink: Story = {
  render: (args) => <TableWithEverything {...args} />,
  args: {
    columns: allColumns,
    data: allPeople,
    enableColumnResizing: true,
  },
}

export const ColumnSpan: Story = {
  args: {
    columns: nestedColumns,
    data: allPeople,
    autoColumnWidth: false,
  },
}

// You can resize and columns scale based on content
export const ResizingOnAutoWidthOn: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    enableColumnResizing: true,
    autoColumnWidth: true,
  },
}

// You can resize, but columns use given initial column widths
export const ResizingOnAutoWidthOff: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    enableColumnResizing: true,
    autoColumnWidth: false,
  },
}

// You can't resize but columns should scale based on content
export const ResizingOffAutoWidthOn: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    enableColumnResizing: false,
    autoColumnWidth: true,
  },
}

// You can't resize but columns use initial size
export const ResizingOffAutoWidthOff: Story = {
  args: {
    columns: allColumns,
    data: allPeople,
    enableColumnResizing: false,
    autoColumnWidth: false,
  },
}
