import React, { FC, ReactNode } from "react"
import { Center, Stack } from "@mantine/core"

const CenterOnPage: FC<{ children: ReactNode }> = ({ children }) => (
  <Center h={"100vh"}>
    <Stack justify={"center"} align={"center"}>
      {children}
    </Stack>
  </Center>
)

export default CenterOnPage
