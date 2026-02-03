import { FC } from "react"
import { Divider, Group, Skeleton } from "@mantine/core"

/**
 * Form loading suspense fallback
 */
const FormLoadingSkeleton: FC = () => (
  <>
    <Skeleton h={20} w={200} />
    <Divider my={15} />
    <Group gap={"sm"}>
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
      <Skeleton h={20} w={200} />
      <Skeleton h={35} mb={10} />
    </Group>
  </>
)

export default FormLoadingSkeleton
