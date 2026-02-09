import React, { FC, PropsWithChildren, Suspense } from "react"
import { LoadingSpinner } from "components/LoadingSpinner"
import CenterOnPage from "components/CenterOnPage"

/**
 * A Suspense component that wraps around components that may have async dependencies.
 */
export const SuspenseSpinner: FC<PropsWithChildren> = ({ children }) => (
  <Suspense
    fallback={
      <CenterOnPage>
        <LoadingSpinner />
      </CenterOnPage>
    }>
    {children}
  </Suspense>
)
