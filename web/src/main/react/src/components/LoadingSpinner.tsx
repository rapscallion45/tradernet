import React, { FC } from "react"
import { Loader, Stack } from "@mantine/core"

/**
 * Component that shows a loading spinner. Where possible, this component should not be used directly, but via the **LoadingResources** component instead.
 */
export const LoadingSpinner: FC = () => (
  <Stack h={"100%"} w={"100%"} justify={"center"} align={"center"}>
    <Loader type={"bars"} color={"secondary"} data-testid="loading-spinner" />
  </Stack>
)
