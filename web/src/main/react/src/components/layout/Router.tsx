import { createBrowserRouter, isRouteErrorResponse, useRouteError } from "react-router-dom"
import { Badge, Card, Center, Group, Stack, Text, Title } from "@mantine/core"
import Routes from "global/Routes"
import Dashboard from "pages/Dashboard/DashboardPage"
import LoginPage from "pages/Login/LoginPage"
import ResetPasswordPage from "pages/ResetPassword/ResetPasswordPage"
import Layout from "components/layout/Layout"
import AuthGateway from "components/AuthGateway"

/**
 * Helper component for handling router errors
 */
const RouteErrorBoundary = () => {
  /** Annoyingly, this returns an unknown */
  const error = useRouteError()
  const message = error instanceof Error ? error.message : JSON.stringify(error, null, 2)
  console.error("Error caught in Router.tsx", message)

  /** If this is a regular error, let it bubble up by throwing it again */
  if (!isRouteErrorResponse(error)) throw error

  /** In the case where we the route simply doesn't exist, we can show a 404. */
  return (
    <Center my={"xl"}>
      <Card padding={"lg"}>
        <Stack>
          <Group justify={"space-between"}>
            <Title order={3}>Page Not Found</Title>
            <Badge color={"orange"}>Warning</Badge>
          </Group>
          <Text>Page does not exist. Please check the address and try again.</Text>
        </Stack>
      </Card>
    </Center>
  )
}

/**
 * Define all application routes
 */
export const router = createBrowserRouter([
  {
    path: Routes.Dashboard,
    ErrorBoundary: RouteErrorBoundary,
    element: (
      <AuthGateway>
        <Layout />
      </AuthGateway>
    ),
    children: [
      {
        path: Routes.Dashboard,
        element: <Dashboard />,
      },
    ],
  },
  {
    path: Routes.Login,
    element: <LoginPage />,
  },
  {
    path: Routes.ResetPassword,
    element: <ResetPasswordPage />,
  },
])
