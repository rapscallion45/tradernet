import type { TestRunnerConfig } from "@storybook/test-runner"

const disabledTests = ["SegmentedControl"]

/** Specific disabled story snapshots */
const disabledStorySnapshots = [
  "Tradernet/AppLayout/All Elements", //Disabled due to flaky test with Mantine AppShell "data-resizing" attribute
  "Tradernet/AppLayout/Header Only", //Disabled due to flaky test with Mantine AppShell "data-resizing" attribute
  "Tradernet/AppLayout/Header And Sidebar", //Disabled due to flaky test with Mantine AppShell "data-resizing" attribute
  "Tradernet/AppLayout/Header And Footer", //Disabled due to flaky test with Mantine AppShell "data-resizing" attribute
  "Tradernet/AppLayout/Side Drawer Simple",
  "Tradernet/AppLayout/Side Drawer With Footer",
  "Tradernet/AppLayout/Side Drawer On Left",
  "Tradernet/AppLayout/Side Drawer With Subtle",
  "Tradernet/AppLayout/Side Drawer With Tabs",
  "Tradernet/AppLayout/Side Drawers For Products",
  "Tradernet/Sidebar/With Menu Scroll", //Disabled due to flaky test with Mantine ScrollArea scrollbar style attribute
]

/**
 * Test Runner configuration
 *
 * Enables snapshot testing
 */
const config: TestRunnerConfig = {
  async postVisit(page, context) {
    /** check if this test suite is disabled, if so skip */
    if (disabledTests.some((tdt) => context.title.includes(tdt))) {
      console.log(`Tests for ${context.title}/${context.name} disabled`)
      return
    }

    /** check if snapshot for this particular story is disabled, if so skip */
    if (disabledStorySnapshots.some((tdt) => `${context.title}/${context.name}`.includes(tdt))) {
      console.log(`Snapshot for ${context.title}/${context.name} disabled`)
      return
    }

    /** The #storybook-root element wraps each story */
    const elementHandler = await page.$("#storybook-root")
    const innerHTML = await elementHandler?.innerHTML()
    // @ts-ignore
    expect(innerHTML).toMatchSnapshot()
  },
}

export default config
