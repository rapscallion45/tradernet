import React, { FC, ReactNode, Suspense } from "react"
import { isAxiosError } from "axios"
import { ErrorBoundary } from "react-error-boundary"
import { QueryErrorResetBoundary } from "@tanstack/react-query"
import { Navigate, RouterProvider } from "react-router-dom"
import useSession from "hooks/useSession"
import { router } from "components/layout/Router"
import MainErrorBoundary from "components/MainErrorBoundary"
import MainSuspenseBoundary from "components/MainSuspenseBoundary"
import Providers from "global/Providers"
import Routes from "global/Routes"

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
          {/** User session management */}
          <SessionBoundary>
            <SessionGate>
              <RouterProvider router={router} />
            </SessionGate>
          </SessionBoundary>
        </Suspense>
      </ErrorBoundary>
    </Providers>
  </div>
)

export default App

/**
 * SessionBoundary is responsible for handling errors when initializing the session.
 */
export const SessionBoundary: FC<{ children: ReactNode }> = ({ children }) => (
  <QueryErrorResetBoundary>
    {({ reset }) => (
      <ErrorBoundary
        onReset={reset}
        FallbackComponent={({ error }: { error: Error; resetErrorBoundary: () => void }) => {
          return isAxiosError(error) ? (
            error.response?.status === 401 ? (
              <Navigate to={Routes.Login} replace state={{ from: location }} />
            ) : error.response?.status === 403 ? (
              <Navigate to={Routes.Login} replace state={{ from: location }} />
            ) : (
              <MainErrorBoundary title={error.response?.statusText ?? "Error"} message={error.message} />
            )
          ) : (
            <MainErrorBoundary title={"Unhandled Error"} message={error.message} />
          )
        }}>
        {children}
      </ErrorBoundary>
    )}
  </QueryErrorResetBoundary>
)

/**
 * SessionGate is responsible for monitoring the session and user data.
 * If the request returns a 401 Unauthorized, the user is redirected to the login page.
 */
export const SessionGate: FC<{ children: ReactNode }> = ({ children }) => {
  useSession()
  return children
}
