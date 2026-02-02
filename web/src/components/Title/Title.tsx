import { FC, ReactNode } from "react"
import { Title as MantineTitle } from "@mantine/core"
import classes from "./Title.module.css"

export type TitleProps = {
  children: string
  highlight?: string | number
  icon?: ReactNode
  firstMatch?: boolean
  matchCase?: boolean
  subtitle?: boolean
}

const color = "light-dark(var(--formpipe-purple-light), var(--formpipe-purple-dark))"

function splitTitleFromWord(title: string, firstMatch: boolean, matchCase: boolean, highlight: string) {
  let matched = false
  const regex = new RegExp(`(${highlight})`, `${firstMatch ? "g" : ""}${matchCase ? "" : "i"}`)
  return title
    .split(regex)
    .filter((x) => x.length > 0)
    .map((x, i) => {
      if (regex.test(x) && !matched) {
        matched = firstMatch
        return (
          <span key={`title-${i}`} style={{ color }}>
            {x}
          </span>
        )
      }
      return <span key={`title-${i}`}>{x}</span>
    })
}

const splitTitleFromIndex = (title: string, highlight: number) =>
  title.split(" ").map((word, i) =>
    // I think this is a rare instance where it makes sense to use a 1-based index
    i + 1 === highlight ? (
      <span key={`title-${i}`} style={{ color }}>
        {word}
      </span>
    ) : (
      <span key={`title-${i}`}> {word} </span> // spaces added to separate the words
    ),
  )

/**
 * Title component with highlighting feature
 * @param children - The title text
 * @param highlight - The text to highlight, or the index of a word to highlight
 * @param icon - The iconType to display next to the title
 * @param firstMatch - Whether the first word should be highlighted or all matches
 * @param matchCase - Whether the matching should be case-sensitive
 * @param subtitle - If the title is a subtitle
 */
const Title: FC<TitleProps> = ({ children, icon, highlight, firstMatch = true, matchCase = false, subtitle = false }) => {
  // If an icon is provided, build the icon component
  const iconComponent = icon ? <span style={{ marginRight: subtitle ? 10 : 15 }}>{icon}</span> : null

  // Either just return the children, split the title from the index, or split the title from the word
  const textComponent = !highlight
    ? children
    : typeof highlight === "number"
      ? splitTitleFromIndex(children, highlight)
      : splitTitleFromWord(children, firstMatch, matchCase, highlight)

  return (
    <MantineTitle classNames={classes} order={subtitle ? 2 : 1}>
      {iconComponent}
      {textComponent}
    </MantineTitle>
  )
}

export default Title
