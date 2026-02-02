import { useEffect } from "react"

/**
 * Prevents background scrolling when `isLocked` is true.
 *
 * Automatically restores scrolling when unmounted.
 */
export function useLockBodyScroll(isLocked: boolean) {
  useEffect(() => {
    if (isLocked) {
      document.body.style.overflow = "hidden"
    } else {
      document.body.style.overflow = ""
    }

    // clean up
    return () => {
      document.body.style.overflow = ""
    }
  }, [isLocked])
}
