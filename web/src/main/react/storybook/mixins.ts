import { AppLength, SidebarItem } from "global/types"
import { InputType } from "storybook/internal/types"
import { faker } from "@faker-js/faker"

// various text options to be used in stories that need text
export const textOptions = [
  "Some short text",
  "Some text that is a bit longer, but not massively long",
  "Some text that is very long and may wrap onto multiple lines. This is a test of the card component, because the card component needs to be able to handle content of all types and sizes.",
  "This text is so darn long that it will wrap onto multiple lines and be very difficult to read. This is not just a test of the card component, but also a test of the limits of human patience. It also tests the limits of my ability to write filler text, as well as the limits of the English language. It's testing so many things in so many ways, it really is quite testing.",
]

// prettier-ignore
export const lengthOptions: AppLength[] = [
    "100px", "200px", "400px", "600px", "1000px",
    "10rem", "20rem", "40rem", "60rem", "100rem",
    "10%", "20%", "40%", "60%", "100%",
    100, 200, 400, 600, 1000
]

// An icon control for use in stories where we require setting an icon
export const iconControl: InputType = {
  control: "select",
  table: {
    type: { summary: "FormpipeIcon" },
  },
}

// A length control for use in stories where we require setting the height, width, etc.
export const lengthControl: InputType = {
  control: "select",
  options: lengthOptions,
  table: {
    type: { summary: "FormpipeLength", detail: `"__rem" | "__%" | "__px" | number` },
    category: "Layout",
  },
}

// A spacing control for use in stories where we require setting padding or margins
export const spacingControl: InputType = {
  control: "select",
  options: ["xs", "sm", "md", "lg", "xl", "xxl"],
  table: {
    type: { summary: "FormpipeSpacing", detail: '"xs" | "sm" | "md" | "lg" | "xl" | "xxl"' },
    category: "Layout",
  },
}

// A size control for use in stories where we require setting a size
export const sizeControl: InputType = {
  control: "inline-radio",
  options: ["xs", "sm", "md", "lg", "xl"],
  table: {
    type: { summary: "FormpipeSize", detail: '"xs" | "sm" | "md" | "lg" | "xl"' },
    category: "Visual",
  },
}

// A control for setting a variant
export const variantControl: InputType = {
  control: "inline-radio",
  options: ["filled", "outline", "subtle"],
  table: {
    type: { summary: "FormpipeVariant", detail: '"filled" | "outline" | "subtle"' },
    category: "Visual",
  },
}

// A control for setting a color
export const colorControl: InputType = {
  control: "select",
  options: ["primary", "secondary", "navy", "blue", "teal", "green", "yellow", "orange", "red", "pink", "gray", "dark"],
  table: {
    type: {
      summary: "FormpipeColor",
      detail: '"primary" | "secondary" | "navy" | "blue" | "teal" | "green" | "yellow" | "orange" | "red" | "pink" | "gray" | "dark"',
    },
    category: "Visual",
  },
}

// region Person Data

export type Person = {
  id: number
  name: string
  dob: Date
  country: string
  favouriteNumber: number
  email: string
  bio: string
  available: boolean
}

// set Faker to always return the same data
faker.seed(123)

// Generate some fake data
const generatePeople = (length: number): Person[] =>
  Array.from({ length }, (_, i) => ({
    id: i + 1,
    name: faker.person.fullName(),
    dob: faker.date.birthdate({ mode: "age", refDate: "2023-01-01T00:00:00.000Z" }), // sets a ref date to ensure consistency
    country: faker.location.country(),
    favouriteNumber: faker.number.int({ min: 1, max: 100 }),
    email: faker.internet.email(),
    bio: faker.person.bio(),
    available: true,
  }))

export const allPeople: Person[] = generatePeople(100)
export const unknownPeople: Person[] = allPeople.slice(0, Math.random() * 75 + 25) // between 25 and 100
export const somePeople = allPeople.slice(0, 8) // 8 is less than page size of 10
export const onePerson = allPeople[0]

// Useful for testing/debugging unknown data amounts, assuming a page size of 10.
// console.debug(`Length of data = ${unknownPeople.length}. Pages of data expected = ${Math.ceil(unknownPeople.length / 10)}`)

// endregion

// region Sidebar Items

// A mock sidebar menu items array */
export const sidebarNavItems: SidebarItem[] = [
  {
    label: "Home",
    icon: "house-chimney",
    path: "/home",
  },
  {
    label: "Profile",
    icon: "user",
    subItems: [
      {
        label: "Profile Settings",
        icon: "gears",
        path: "/profile/settings",
      },
      {
        label: "Logout",
        icon: "sign-in",
        path: "/logout",
      },
    ],
  },
  {
    label: "Manage Environments",
    icon: "globe",
    path: "/manage-environments",
  },
  {
    label: "Invoices",
    icon: "file",
    path: "/invoices",
    subItems: [
      {
        label: "Invoice Settings",
        icon: "gears",
        path: "/invoices/settings",
      },
    ],
  },
  {
    label: "All Users",
    icon: "users",
    path: "/all-users",
    subItems: [
      {
        label: "Manage Users",
        icon: "gears",
        path: "/manage-users",
      },
      {
        label: "Security Roles",
        icon: "lock-keyhole",
        path: "/security-roles",
      },
    ],
  },
  {
    label: "Tickets",
    icon: "ticket",
    path: "/tickets",
  },
  {
    label: "Notifications",
    icon: "bell",
    path: "/notifications",
  },
  {
    label: "Input Apps",
    icon: "barcode",
    path: "/input-apps",
    subItems: [
      {
        label: "Sub Menu 1",
        icon: "gears",
        path: "/input-apps/sub-menu-1",
      },
      {
        label: "Sub Menu 2",
        icon: "lock-keyhole",
        path: "/input-apps/sub-menu-2",
      },
    ],
  },
  {
    label: "Messages",
    icon: "message",
    path: "/messages",
    subItems: [
      {
        label: "Sub Menu 1",
        icon: "gears",
        path: "/messages/sub-menu-1",
      },
      {
        label: "Sub Menu 2",
        icon: "lock-keyhole",
        path: "/messages/sub-menu-2",
      },
    ],
  },
  {
    label: "Search Apps",
    icon: "magnifying-glass",
    path: "/search-apps",
  },
  {
    label: "API Keys",
    icon: "key",
    path: "/api-keys",
    subItems: [
      {
        label: "Sub Menu 1",
        icon: "gears",
        path: "/api-keys/sub-menu-1",
      },
      {
        label: "Sub Menu 2",
        icon: "lock-keyhole",
        path: "/api-keys/sub-menu-2",
      },
    ],
  },
  {
    label: "Reports",
    icon: "triangle-exclamation",
    path: "/reports",
    subItems: [
      {
        label: "Sub Menu 1",
        icon: "gears",
        path: "/reports/sub-menu-1",
      },
      {
        label: "Sub Menu 2",
        icon: "lock-keyhole",
        path: "/reports/sub-menu-2",
      },
    ],
  },
  {
    label: "Dashboard",
    icon: "object-group",
    path: "/dashboard",
    subItems: [
      {
        label: "Sub Menu 1",
        icon: "gears",
        path: "/dashboard/sub-menu-1",
      },
      {
        label: "Sub Menu 2",
        icon: "lock-keyhole",
        path: "/dashboard/sub-menu-2",
      },
    ],
  },
]

// endregion
