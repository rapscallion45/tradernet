import { Stack, Tabs } from "@mantine/core"
import { FC, useState } from "react"
import Button from "components/Button/Button"

/**
 * Tabs are showcased here with a simple example and a more complex wizard example.
 *
 * We do not have our own Tabs component as it is not needed. Simply using the Mantine tabs will use the correct styles, as set in the theme.
 */
const meta = {
  title: "Mantine/Tabs",
}

export default meta

const TabList: FC<{ grow?: boolean }> = ({ grow }) => (
  <Tabs defaultValue={"basic"}>
    <Tabs.List grow={grow}>
      <Tabs.Tab value={"basic"}>Basic details</Tabs.Tab>
      <Tabs.Tab value={"doc-defs"}>Document definitions</Tabs.Tab>
      <Tabs.Tab value={"query"}>Query</Tabs.Tab>
      <Tabs.Tab value={"settings"}>Settings</Tabs.Tab>
      <Tabs.Tab value={"results"}>Results</Tabs.Tab>
      <Tabs.Tab value={"editable"}>Editable keys</Tabs.Tab>
      <Tabs.Tab value={"access"}>Access</Tabs.Tab>
    </Tabs.List>
    <Tabs.Panel value={"basic"}>Content for Basic Details</Tabs.Panel>
    <Tabs.Panel value={"doc-defs"}>Content for Document Definitions</Tabs.Panel>
    <Tabs.Panel value={"query"}>Content for Query</Tabs.Panel>
    <Tabs.Panel value={"settings"}>Content for Settings</Tabs.Panel>
    <Tabs.Panel value={"results"}>Content for Results</Tabs.Panel>
    <Tabs.Panel value={"editable"}>Content for Editable Keys</Tabs.Panel>
    <Tabs.Panel value={"access"}>Content for Access</Tabs.Panel>
  </Tabs>
)

export const Default = () => <TabList />

export const Growing = () => <TabList grow />

// Simple wizard implementation to showcase moving through tabs and maintaining 'enabled' state
export const Wizard = () => {
  const [enabledTabs, setEnabledTabs] = useState(["basic"])
  const [activeTab, setActiveTab] = useState<string | null>("basic")

  // Helper function to create a tab with a disabled state
  const Tab = (name: string, content: string) => (
    <Tabs.Tab disabled={!enabledTabs.includes(name)} value={name}>
      {content}
    </Tabs.Tab>
  )

  // Helper function to create a panel with a 'next' button that enables the next tab and switches to it
  const Panel = (name: string, next: string, content: string) => (
    <Tabs.Panel value={name}>
      <Stack align={"flex-start"}>
        {content}
        <Button
          onClick={() => {
            setEnabledTabs([...enabledTabs, next])
            setActiveTab(next)
          }}>
          Next Tab
        </Button>
      </Stack>
    </Tabs.Panel>
  )

  return (
    <Tabs value={activeTab} onChange={setActiveTab}>
      <Tabs.List>
        {Tab("basic", "Basic details")}
        {Tab("doc-defs", "Document definitions")}
        {Tab("query", "Query")}
        {Tab("settings", "Settings")}
        {Tab("results", "Results")}
        {Tab("editable", "Editable keys")}
        {Tab("access", "Access")}
      </Tabs.List>
      {Panel("basic", "doc-defs", "Content for Basic Details")}
      {Panel("doc-defs", "query", "Content for Document Definitions")}
      {Panel("query", "settings", "Content for Query")}
      {Panel("settings", "results", "Content for Settings")}
      {Panel("results", "editable", "Content for Results")}
      {Panel("editable", "access", "Content for Editable Keys")}
      {Panel("access", "basic", "Content for Access")}
    </Tabs>
  )
}
