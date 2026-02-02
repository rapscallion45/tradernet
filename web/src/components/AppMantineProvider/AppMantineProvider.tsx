import { FC, ReactNode } from "react"
import {
  ActionIcon,
  Checkbox,
  CheckboxGroup,
  ColorSchemeScript,
  CSSVariablesResolver,
  FileInput,
  Input,
  InputWrapper,
  MantineColorScheme,
  MantineProvider,
  MantineThemeOverride,
  MultiSelect,
  Notification,
  Progress,
  SegmentedControl,
  Select,
  Tabs,
} from "@mantine/core"
import "@fontsource-variable/dm-sans"
import "@fontsource-variable/lexend"
import "@fontsource/dm-mono"
import "@mantine/core/styles.css"
import "@mantine/dates/styles.css"
import "@mantine/notifications/styles.css"
import actionIconClasses from "./ActionIcon.module.css"
import checkboxClasses from "./Checkbox.module.css"
import inputClasses from "./Input.module.css"
import inputWrapperClasses from "./InputWrapper.module.css"
import datePickerClasses from "./DatePicker.module.css"
import segmentedControlClasses from "./SegmentedControl.module.css"
import selectClasses from "./Select.module.css"
import notificationClasses from "./Notification.module.css"
import progressClasses from "./Progress.module.css"
import tabsClasses from "./Tabs.module.css"
import "global/global.css"
import { DatePickerInput, DateTimePicker, TimeInput } from "@mantine/dates"
import { Notifications } from "@mantine/notifications"
import { IconArrowLeft, IconArrowRight, IconCalendar, IconCaretDown, IconCheck, IconCircleLetterI, IconClock, IconFolderOpen } from "@tabler/icons-react"

// leave it on one line, prettier!
// prettier-ignore
export const colors = {
    primary: ["#feeaff", "#f4d2fd", "#e5a4f7", "#d672f1", "#c948ed", "#c12eea", "#be1fea", "#a713d0", "#950bbb", "#8200a4"],
    secondary: ["#f4ebff", "#e4d1fb", "#c89ef9", "#a967f8", "#913bf7", "#8121f7", "#7916f8", "#680cdd", "#5b07c5", "#4e00ad"],
    navy: ["#ebeffe", "#d3daf8", "#a3b2f4", "#7087f1", "#4763ef", "#314cee", "#2741ef", "#1d33d5", "#152ebf", "#0627a7"],
    blue: ["#ebf8ff", "#d6edfa", "#a7daf8", "#76c6f6", "#55b5f5", "#44abf5", "#3ba5f6", "#2f90db", "#2281c4", "#006fad"],
    teal: ["#e2fffd", "#d2f8f6", "#aaeee9", "#7ee5dd", "#5bddd3", "#43d8cd", "#32d6c9", "#1ebdb1", "#02a99d", "#009389"],
    green: ["#f4fde7", "#e8f8d4", "#d1f1aa", "#b9e97c", "#a3e256", "#96de3d", "#8fdc2f", "#7bc221", "#6cac18", "#5a9508"],
    yellow: ["#fffbe0", "#fff6ca", "#ffed99", "#ffe362", "#ffda36", "#ffd518", "#ffd201", "#e3b900", "#caa500", "#ae8e00"],
    orange: ["#fff2e4", "#ffe4cf", "#fcc79e", "#faa869", "#f98e3e", "#f87d22", "#f87412", "#dd6306", "#c55600", "#ac4800"],
    red: ["#ffe8ee", "#ffd1d9", "#fba1b1", "#f66d86", "#f24261", "#f0274a", "#f0163e", "#d60530", "#c00029", "#a80021"],
    pink: ["#ffe9f7", "#ffd3e6", "#f7a4c8", "#f173a9", "#eb498f", "#e92f7f", "#e82077", "#cf1165", "#ba075a", "#a3004d"],
}
const appTheme: MantineThemeOverride = {
  primaryColor: "secondary",
  primaryShade: 8,
  // @ts-expect-error it's easier to leave colors untyped, Mantine knows what it's doing.
  colors: colors,
  fontFamily: "DM Sans Variable, Arial, sans-serif",
  headings: {
    fontFamily: "Lexend Variable, Arial, sans-serif",
  },
  fontFamilyMonospace: "DM Mono, Consolas, monospace",
  fontSmoothing: true,
  autoContrast: true, // automatically makes text black for lighter colours
  luminanceThreshold: 0.4, // threshold for the above behaviour
  spacing: {
    xs: "0.25rem",
    sm: "0.5rem",
    md: "1rem",
    lg: "2rem",
    xl: "4rem",
    xxl: "8rem",
  },
  defaultRadius: "sm", // 4px
  white: "#fff",
  black: "#222",
  cursorType: "pointer",
  breakpoints: {
    // Must match postcss.config.cjs!
    xs: "36em", // 576px
    sm: "48em", // 768px
    md: "62em", // 992px
    lg: "75em", // 1200px
    xl: "88em", // 1408px
    xxl: "112.5em", // 1800px
  },

  // Customise components globally
  components: {
    ActionIcon: ActionIcon.extend({
      classNames: actionIconClasses,
    }),
    Tooltip: {
      defaultProps: {
        radius: "sm",
        p: "sm",
        arrowSize: 8,
      },
    },
    Checkbox: Checkbox.extend({
      classNames: checkboxClasses,
      defaultProps: {
        size: "sm",
        icon: ({ indeterminate }) => (indeterminate ? <IconCheck /> : <IconCheck />),
      },
    }),
    CheckboxGroup: CheckboxGroup.extend({
      styles: {
        label: {
          fontWeight: 700,
          fontFamily: "var(--mantine-font-family-headings)",
        },
      },
    }),
    Tabs: Tabs.extend({
      classNames: tabsClasses,
    }),
    SegmentedControl: SegmentedControl.extend({
      classNames: segmentedControlClasses,
      defaultProps: {
        withItemsBorders: false,
        size: "md",
      },
    }),
    Input: Input.extend({
      classNames: inputClasses,
    }),
    InputWrapper: InputWrapper.extend({
      classNames: inputWrapperClasses,
    }),
    FileInput: FileInput.extend({
      defaultProps: {
        leftSection: <IconFolderOpen />,
        placeholder: "Select file",
        clearable: true,
      },
    }),
    TimeInput: TimeInput.extend({
      defaultProps: {
        leftSection: <IconClock />,
      },
    }),
    DatePickerInput: DatePickerInput.extend({
      classNames: datePickerClasses,
      defaultProps: {
        clearable: true,
        placeholder: "Select date",
        leftSection: <IconCalendar />,
        leftSectionPointerEvents: "none",
        previousIcon: <IconArrowLeft />,
        nextIcon: <IconArrowRight />,
      },
    }),
    DateTimePicker: DateTimePicker.extend({
      classNames: datePickerClasses,
      defaultProps: {
        clearable: true,
        placeholder: "Select date and time",
        leftSection: <IconCalendar />,
        leftSectionPointerEvents: "none",
        previousIcon: <IconArrowLeft />,
        nextIcon: <IconArrowRight />,
        submitButtonProps: {
          variant: "outline",
        },
      },
    }),
    Select: Select.extend({
      classNames: selectClasses,
      defaultProps: {
        rightSection: <IconCaretDown />,
        checkIconPosition: "right",
      },
    }),
    MultiSelect: MultiSelect.extend({
      classNames: selectClasses,
      defaultProps: {
        rightSection: <IconCaretDown />,
        checkIconPosition: "right",
      },
    }),
    Notification: Notification.extend({
      classNames: notificationClasses,
      defaultProps: {
        variant: "info",
        p: "md",
        icon: <IconCircleLetterI />,
      },
    }),
    Progress: Progress.extend({
      classNames: progressClasses,
      defaultProps: {
        radius: "xl",
        size: "sm",
      },
    }),
  },
}

// Leaving this here for now as it will likely be useful later on
const resolver: CSSVariablesResolver = (theme) => ({
  variables: {},
  light: {
    "--mantine-color-text": "#222",
    "--mantine-color-body": theme.colors.gray[0],
    "--mantine-color-error": theme.colors.red[8],
    "--mantine-color-success": theme.colors.green[8],
    "--input-asterisk-color": theme.colors.secondary[8],
  },
  dark: {
    "--mantine-color-text": "#fff",
    "--mantine-color-body": theme.colors.dark[8],
    "--mantine-color-error": theme.colors.red[6],
    "--mantine-color-success": theme.colors.green[6],
    "--input-asterisk-color": theme.colors.secondary[4],
  },
})

export const AppMantineProvider: FC<{
  defaultColorScheme?: MantineColorScheme
  children: ReactNode
}> = ({ defaultColorScheme, children }) => (
  <>
    <ColorSchemeScript defaultColorScheme={defaultColorScheme ?? "auto"} />
    <MantineProvider defaultColorScheme={defaultColorScheme ?? "auto"} theme={appTheme} cssVariablesResolver={resolver}>
      <Notifications position={"top-center"} />
      {children}
    </MantineProvider>
  </>
)
