import { create } from "zustand"
import { User } from "api/types"
import { SideDrawerLocation } from "global/types"

/**
 * Global store for managing global application state.
 *
 * @param currentUser The current user's basic information.
 * @param setCurrentUser Function to set the current user.
 * @param sidebarExpanded The current "expanded" state of the AppLayout Sidebar.
 * @param setSidebarExpanded Function to set the expanded state of the AppLayout Sidebar.
 */
type GlobalStore = {
  currentUser: User
  setCurrentUser: (userInfo: User) => void
  sidebarExpanded: boolean
  setSidebarExpanded: (expanded: boolean) => void
  activeHeaderPanel: string | undefined
  setActiveHeaderPanel: (panel: string | undefined) => void
  activeLeftPanel: string | undefined
  setActiveLeftPanel: (panel: string | undefined) => void
  activeRightPanel: string | undefined
  setActiveRightPanel: (panel: string | undefined) => void
  pinnedPanel: Record<SideDrawerLocation, string | undefined>
  setPinnedPanel: (region: SideDrawerLocation, name: string | undefined) => void
}

export const useGlobalStore = create<GlobalStore>((set) => ({
  // User Info
  currentUser: { username: "" },
  setCurrentUser: (userInfo: User) => set(() => ({ currentUser: userInfo })),
  // AppLayout Sidebar
  sidebarExpanded: false,
  setSidebarExpanded: (expanded: boolean) => set(() => ({ sidebarExpanded: expanded })),
  // Header Panel
  activeHeaderPanel: undefined,
  setActiveHeaderPanel: (panel: string | undefined) => {
    console.log("Setting active header panel to:", panel)
    set(() => ({ activeHeaderPanel: panel }))
  },
  // Left Panel
  activeLeftPanel: undefined,
  setActiveLeftPanel: (panel: string | undefined) => {
    console.log("Setting active left panel to:", panel)
    set(() => ({ activeLeftPanel: panel }))
  },
  // Right Panel
  activeRightPanel: undefined,
  setActiveRightPanel: (panel: string | undefined) => {
    console.log("Setting active right panel to:", panel)
    set(() => ({ activeRightPanel: panel }))
  },

  pinnedPanel: {
    header: undefined,
    left: undefined,
    right: undefined,
  },

  setPinnedPanel: (region, name) => set((state) => ({ pinnedPanel: { ...state.pinnedPanel, [region]: name } })),
}))
