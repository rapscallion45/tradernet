import { FC, ReactNode } from "react"
import { Group, Image, useComputedColorScheme } from "@mantine/core"
// import LogoLightMode from "../../../public/symbol.svg"
// import LogoDarkMode from "../../../public/symbol-white.svg"
import { useIsMobile } from "hooks/useIsMobile"

/**
 * Header Logos props
 * @param logoLightModeSrc - application logo component to be displayed right of Tradernet "crest" logo in "light mode"
 * @param logoDarkModeSrc - application logo component to be displayed right of Tradernet "crest" logo in "dark mode"
 * @param title - a fallback component to display in place of application logo if not provided
 * @param logoAlt - alt text for the application logo (accessibility)
 */
export type HeaderLogoProps = {
  logoLightModeSrc?: string
  logoDarkModeSrc?: string
  title?: ReactNode
  logoAlt?: string
}

/**
 * Header Logos component that is used for rendering the Tradernet "crest" logo and either passed application logo
 * or passed title component
 */
const HeaderLogos: FC<HeaderLogoProps> = ({ logoLightModeSrc, logoDarkModeSrc, title, logoAlt }) => {
  const colorScheme = useComputedColorScheme()
  const isLightMode = colorScheme === "light"
  const isDarkMode = !isLightMode
  const isMobile = useIsMobile()

  return (
    <Group gap={5} wrap={"nowrap"} style={isMobile ? undefined : { cursor: "pointer" }} hiddenFrom={"sm"}>
      {/*<Image p={"sm"} src={isDarkMode ? LogoDarkMode : LogoLightMode} h={50} w={"auto"} fit={"contain"} alt="Tradernet crest logo" />*/}
      {logoLightModeSrc && isLightMode ? <Image p={0} src={logoLightModeSrc} fit={"contain"} h={18} w={"auto"} alt={logoAlt} /> : isLightMode && title}
      {logoDarkModeSrc && isDarkMode ? <Image p={0} src={logoDarkModeSrc} fit={"contain"} h={18} w={"auto"} alt={logoAlt} /> : isDarkMode && title}
    </Group>
  )
}

export default HeaderLogos
