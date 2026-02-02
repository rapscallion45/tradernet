import { FC, Suspense } from "react"
import { ErrorBoundary } from "react-error-boundary"
import { RouterProvider } from "react-router-dom"
import { router } from "components/layout/Router"
import Providers from "global/Providers"
import MainErrorBoundary from "components/MainErrorBoundary"
import MainSuspenseBoundary from "components/MainSuspenseBoundary"

/**
 * Application entry point
 */
const App: FC = () => (
  <div data-testid={"app"}>
    <Providers>
      {/** Catch all for any errors that are not handled lower down the tree */}
      <ErrorBoundary fallback={<MainErrorBoundary />}>
        {/** Main suspense loading spinner */}
        <Suspense fallback={<MainSuspenseBoundary />}>
          <RouterProvider router={router} />
        </Suspense>
      </ErrorBoundary>
    </Providers>
  </div>
)

export default App
