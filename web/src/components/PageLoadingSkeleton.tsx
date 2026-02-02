import { FC } from "react"
import { Box, Flex, Group, Skeleton } from "@mantine/core"

/**
 * Page loading suspense fallback
 */
const PageLoadingSkeleton: FC = () => (
  <>
    <Flex direction={"column"} gap={"lg"}>
      <Flex justify={"start"} gap={"md"}>
        <Skeleton w={150} h={40} />
        <Skeleton w={200} h={40} />
        <Skeleton w={300} h={40} />
        <Skeleton w={200} h={40} />
        <Box style={{ flexGrow: 1 }} />
        <Group gap={"sm"}>
          <Skeleton w={120} h={40} />
          <Skeleton w={120} h={40} />
        </Group>
      </Flex>
      <Skeleton h={650} />
    </Flex>
  </>
)

export default PageLoadingSkeleton
