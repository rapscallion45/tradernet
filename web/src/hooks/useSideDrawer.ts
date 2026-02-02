import { useGlobalStore } from "./useGlobalStore"
import { SideDrawerLocation } from "global/types"
import { useCallback, useMemo } from "react"

/**
 * Convenience hook for using the global store to manage side panels.
 *
 * Usage: `const { opened, toggle, close } = useSideDrawer("my-drawer", "right")`
 *
 * @param name The name of the side panel.
 * @param region The region of the side panel. Can be "header", "left", or "right".
 */
export const useSideDrawer = (name: string, region: SideDrawerLocation) => {
  const store = useGlobalStore()

  const opened = useMemo(() => {
    const activePanel = {
      header: store.activeHeaderPanel,
      left: store.activeLeftPanel,
      right: store.activeRightPanel,
    }[region]

    return activePanel === name || store.pinnedPanel[region] === name
  }, [name, region, store.pinnedPanel, store.activeHeaderPanel, store.activeLeftPanel, store.activeRightPanel])

  const setActivePanel = useCallback<(panel: string | undefined) => void>(
    (panel) => {
      const setter = {
        header: store.setActiveHeaderPanel,
        left: store.setActiveLeftPanel,
        right: store.setActiveRightPanel,
      }[region]

      setter(panel)
    },
    [region, store.setActiveHeaderPanel, store.setActiveLeftPanel, store.setActiveRightPanel],
  )

  const pinned = useMemo(() => store.pinnedPanel[region] === name, [store.pinnedPanel, name, region])

  const open = useCallback(() => {
    setActivePanel(name)
  }, [name, setActivePanel])

  const toggle = useCallback(() => {
    if (opened && !pinned) {
      setActivePanel(undefined)
    } else {
      setActivePanel(name)
    }
  }, [name, opened, pinned, setActivePanel])

  const close = useCallback(() => {
    if (!pinned) {
      setActivePanel(undefined)
    }
  }, [pinned, setActivePanel])

  const pin = useCallback(() => store.setPinnedPanel(region, name), [store.setPinnedPanel, region, name])

  const unpin = useCallback(() => {
    if (store.pinnedPanel[region] === name) {
      store.setPinnedPanel(region, undefined)
    }
  }, [store.pinnedPanel, store.setPinnedPanel, region, name])

  return {
    opened,
    pinned,
    open,
    toggle,
    close,
    pin,
    unpin,
  }
}
