import { FC, ReactNode } from "react"
import { ErrorBoundary } from "react-error-boundary"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { AppMantineProvider } from "components/AppMantineProvider/AppMantineProvider"
import "@mantine/notifications/styles.css"
import "global/global.css"

/** instantiate Query client */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
})

/**
 * Central location for all providers
 */
const Providers: FC<{ children: ReactNode }> = ({ children }) => {
  return (
    <ErrorBoundary fallback={<p>An unexpected error occurred within Providers.tsx</p>}>
      <QueryClientProvider client={queryClient}>
        <AppMantineProvider>{children}</AppMantineProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  )
}

export default Providers
