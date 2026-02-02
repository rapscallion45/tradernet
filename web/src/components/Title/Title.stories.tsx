import { Meta, StoryObj } from "@storybook/react"
import Title from "./Title"
import { iconControl } from "../../storybook/mixins"

/**
 * Tradernet Paper story meta
 */
const meta = {
  title: "Tradernet/Title",
  component: Title,
  parameters: {
    layout: `padded`,
  },
  argTypes: {
    children: {
      control: "text",
      description: "The title text",
    },
    icon: {
      description: "The icon to display next to the title",
      table: {
        ...iconControl.table,
        type: { summary: "FormpipeIcon" },
      },
    },
    highlight: {
      control: {
        type: "text",
      },
      description: "Highlighted word within the title",
      table: {
        type: { summary: "text" },
        category: "Highlighting",
      },
    },
    subtitle: {
      control: { type: "boolean" },
      description: "If the component should display as a subtitle",
      table: {
        type: { summary: "boolean" },
      },
    },
    firstMatch: {
      control: { type: "boolean" },
      description: "Only match the first instance of the highlighted word",
      table: {
        type: { summary: "boolean" },
      },
    },
    matchCase: {
      control: { type: "boolean" },
      description: "Match the case of the highlighted word",
      table: {
        type: { summary: "boolean" },
      },
    },
  },
  args: {
    children: "Tradernet Title Example",
    highlight: "Tradernet",
  },
} satisfies Meta<typeof Title>

export default meta

type Story = StoryObj<typeof Title>

/**
 *  Default story
 */
export const Default: Story = {
  args: {},
}

/**
 *  Highlighting can be removed by setting the highlight to `undefined`
 */
export const NoHighlighting: Story = {
  args: {
    highlight: undefined,
  },
}

/**
 *  Icon can be set to any icon from the `IconType` enum, here it is set to `ThumbsUp`
 */
export const Icon: Story = {
  args: {
    icon: "arrow-right",
  },
}

/**
 *  Subtitle will display the title with a smaller font size
 */
export const Subtitle: Story = {
  args: {
    subtitle: true,
  },
}

/**
 *  Subtitle should scale the icon appropriately
 */
export const SubtitleWithIcon: Story = {
  args: {
    subtitle: true,
    icon: "arrow-right",
  },
}

/**
 *  Empty title string
 */
export const NoTitle: Story = {
  args: {
    children: "",
  },
}

/**
 *  One-word will show the whole title in the set color
 */
export const OneWord: Story = {
  args: {
    children: "Tradernet",
    highlight: "Tradernet",
  },
}

/**
 *  Long titles will be handled the same way as short titles
 */
export const LongTitle: Story = {
  args: {
    children: "Tradernet Title Example with a really long title to test how the component handles it",
    highlight: "Tradernet",
  },
}

/**
 *  When the title has quotations, it will be handled the same way as titles without quotations
 */
export const Quotation: Story = {
  args: {
    children: `"Tradernet Title Example" with quotations`,
    highlight: "Tradernet",
  },
}

/**
 *  When the title has quotations, it will be handled the same way as titles without quotations
 */
export const Welcome: Story = {
  args: {
    children: `Welcome Ben, what would you like to do today?`,
    highlight: "Ben",
  },
}

/**
 *  When the title has multiple matches, only the first will be highlighted by default
 */
export const MultipleMatchesDefault = {
  args: {
    children: `Tradernet, this is Tradernet`,
    highlight: "Tradernet",
  },
}

/**
 *  When the title has multiple matches, you can specify global highlighting
 */
export const MultipleMatchesAll = {
  args: {
    children: `Tradernet, this is Tradernet`,
    highlight: "Tradernet",
    firstMatch: false,
  },
}

/**
 *  The matching should ignore case
 */
export const CaseInsensitiveDefault = {
  args: {
    children: `Tradernet, this is formpipe`,
    highlight: "formpipe",
  },
}

/**
 *  The matching should ignore case
 */
export const CaseSensitive = {
  args: {
    children: `Tradernet, this is formpipe`,
    highlight: "formpipe",
    matchCase: true,
  },
}

export const ByIndex = {
  args: {
    children: "This should highlight the third word",
    highlight: 3,
  },
}

export const ByIndexFirst = {
  args: {
    children: "This should highlight the first word",
    highlight: 1,
  },
}
