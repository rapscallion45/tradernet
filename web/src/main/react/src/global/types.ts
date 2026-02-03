/**
 * Global application types
 */

import { ReactNode } from "react"

/**
 * General-purpose types that serve as a custom API for all components.
 *
 * The idea is to abstract away Mantine as a dependency,
 * so that we can swap it out for another library in the future.
 *
 * This also has the additional benefit of allowing us to further limit the behaviour of our components,
 * as these types tend to be a subset of the Mantine types.
 */

/**
 * AppSpacing is a simple representation of the size of a spacing value.
 * These include padding, margin, gap, etc.
 * Note the addition of "xxl" which is not a default Mantine value.
 */
export type AppSpacing = "xs" | "sm" | "md" | "lg" | "xl" | "xxl"

export type AppSize = "xs" | "sm" | "md" | "lg" | "xl"

export type AppIconSize = AppSize | "2xs" | "2xl"

/**
 * AppLength is a simple representation of a length in rem units.
 * Possible values: 0.2rem, 1rem, 2rem, etc.
 */
export type AppRem = `${number}rem`

/**
 * AppPercentage is a simple representation of a percentage.
 * Possible values: 0.1%, 50%, 100%, etc.
 */
export type AppPercentage = `${number}%`

/**
 * AppPixel is a simple representation of a length in pixel units. Possible values: 1px, 20px, 300px, etc.
 */
export type AppPixel = `${number}px`

/**
 * AppLength is a simple representation of a length in rem units or a percentage,
 * or just a plain number, which will be interpreted as a pixel size.
 */
export type AppLength = AppRem | AppPercentage | AppPixel | number

/**
 * AppColor is a simple representation of a color. This excludes any default Mantine colors,
 * and does not allow hex codes, etc.
 */

export type AppColor = "primary" | "secondary" | "navy" | "blue" | "teal" | "green" | "yellow" | "orange" | "red" | "pink" | "gray" | "dark"

/**
 * AppVariant is a simple representation of a variant.
 */
export type AppVariant = "filled" | "outline" | "subtle"

/**
 * AppNotificationVariant is a simple representation of a notification category.
 * The category passed in determines the variant of the notification, which sets the colour and icon.
 */
export type AppNotificationVariant = "info" | "error" | "success"

/**
 * AppInputVariant is a simple representation of an input variant. This adds a new "outline" variant.
 */
export type AppInputVariant = "default" | "outline" | "filled" | "unstyled"

/**
 * AppNotification is a simple representation of a notification.
 */
export type AppNotification = {
  id: string
  title: string
  message: string | string[]
  variant: AppNotificationVariant
  loading?: boolean
  progress?: number
  timestamp: number
}

/**
 * SidebarItem is a simple representation of a sidebar nav link.
 */
export type SidebarItem = {
  icon?: ReactNode
  label: string
  path?: string
  subItems?: SidebarItem[]
}

/**
 * SideDrawerSection represents a header or footer in a side drawer.
 */
export type SideDrawerSection = {
  content: ReactNode
  subtle?: boolean
}

/**
 * SideDrawerLocation allows drawers in different regions to be managed separately.
 */
export type SideDrawerLocation = "header" | "left" | "right"
