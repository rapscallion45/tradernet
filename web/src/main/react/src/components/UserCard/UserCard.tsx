import React, { FC, useId } from "react"
import { Avatar, Badge, Group, Stack, Text, Tooltip } from "@mantine/core"
import classes from "./UserCard.module.css"
import { BaseCard, BaseCardProps } from "../BaseCard/BaseCard"
import { kebabCase } from "utils/strings"
import { IconCrown, IconUser } from "@tabler/icons-react"

export type UserCardProps = Omit<BaseCardProps, "featured" | "children"> & {
  username: string
  fullName?: string
  isAdmin?: boolean
  groups?: string[]
  avatarUrl?: string
}

/**
 * UserCard component  is a **flexible card** that displays user information including username, full name, admin status, groups, and avatar.
 * It extends BaseCard and includes features like click handling and secondary actions for delete, clone, etc.
 *
 * @param username The username of the user (required).
 * @param fullName The full name of the user (optional).
 * @param isAdmin Boolean indicating if the user has admin privileges (optional).
 * @param groups An array of group names the user belongs to (optional).
 * @param onClick Action to perform when the card is clicked (optional).
 * @param avatarUrl Optional. The URL of the user's avatar image. If not provided, initials will be used.
 * @param rest Any other props to pass to the underlying BaseCard component from BaseCard.
 */
export const UserCard: FC<UserCardProps> = ({ username, fullName, isAdmin, groups = [], onClick, avatarUrl, ...rest }) => {
  const name = fullName ?? username
  const autoId = useId()
  // Assign unique ids for the label and value for accessibility
  const labelId = `user-label-${kebabCase(name)}-${autoId}`
  return (
    <BaseCard key={username} onClick={onClick} classes={classes} aria-labelledby={labelId} {...rest} data-testid={`UserCard-${username}`}>
      <Stack h={"100%"} justify={"space-between"}>
        <Stack align={"stretch"} gap={"sm"}>
          <Group justify={"flex-end"}>{isAdmin ? <IconCrown /> : <IconUser />}</Group>
          <Group align={"flex-start"} justify={"center"}>
            <Avatar
              src={avatarUrl}
              key={name}
              name={name}
              size={100}
              color={"initials"}
              allowedInitialsColors={["primary", "secondary", "navy", "cyan", "blue", "red"]}
            />
          </Group>
          <Stack gap={"xs"} mah={80}>
            <Stack align={"center"} gap={"sm"}>
              <Text fz={"lg"} fw={"bold"} className={classes.truncate} id={labelId}>
                {username}
              </Text>
              <Text fz={"sm"} className={classes.truncate} mih={30}>
                {fullName}
              </Text>
              <Group gap={"xs"} align={"center"} justify={"center"}>
                {groups.length < 4 ? (
                  groups.map((group) => (
                    <Badge color={"blue"} key={group}>
                      {group}
                    </Badge>
                  ))
                ) : (
                  <>
                    {groups.slice(0, 2).map((group) => (
                      <Badge color={"blue"} key={group}>
                        {group}
                      </Badge>
                    ))}
                    <Tooltip label={`${groups.slice(2).join(", ")}.`} position={"top"} withArrow multiline maw={"18rem"}>
                      <Badge color={"blue"} key={`${username}-groups-summary`}>
                        {`${groups.slice(2).length} more`}
                      </Badge>
                    </Tooltip>
                  </>
                )}
              </Group>
            </Stack>
          </Stack>
        </Stack>
      </Stack>
    </BaseCard>
  )
}
