/** The jest-serializer-html package is available as a dependency of the test-runner */
const jestSerializerHtml = require("jest-serializer-html")

/** Local interface for specifying pattern checking properties */
interface IPatternCheckItem {
  match: string | RegExp
  matchCount: number
  prefix?: string
}

/**
 * Custom Storybook snapshot serializer to preprocess the HTML before it is passed to the Test Runner
 */
module.exports = {
  /**
   * The test-runner calls the serialize function when the test reaches the expect(SomeHTMLElement).toMatchSnapshot().
   * It will replace all dynamic IDs and class names with static placeholders so that the snapshot is consistent.
   * For instance, from:
   *    <input id="mantine-jdkmtnaku" class="m_46b77525" placeholder="Some input..."/>
   * to:
   *    <input id="mantine-mocked-id-0" class="m_0" placeholder="Some input..." />
   */
  serialize(val: string): string {
    return jestSerializerHtml.print(replaceDynamicIDsAndClassNames(val))
  },
  test(val: string): string {
    return jestSerializerHtml.test(val)
  },
}

/**
 * Replaces all dynamically generated ID and class names within snapshot
 * @param {string} markup - snapshot HTML markup to be processed
 * @returns {string} finalised HTML markup with ID and class name replacements
 */
const replaceDynamicIDsAndClassNames = (markup: string): string => {
  /** Define our class name pattern checks */
  const classNameChecks: IPatternCheckItem[] = [
    { match: "m_", matchCount: 0 } /** Mantine dynamic class names */,
    { match: "__m__-", matchCount: 0 } /** Mantine dynamic class names */,
    { match: /_([a-zA-Z0-9])+_/g, matchCount: 0 } /** CSS Module dynamic class names */,
    { match: "sc-", matchCount: 0 } /** Styled Components dynamic class names */,
  ]

  /** Define our ID name pattern checks */
  const idChecks: IPatternCheckItem[] = [{ match: "mantine-", matchCount: 0 } /** Mantine dynamic ID names */]

  /** Keep track of ID and class names we've already seen */
  const classNameMap: Record<string, string> = {}
  const idMap: Record<string, string> = {}

  return (
    removeMantineTags(markup)
      /** Cycle through classes and replace matching patterns */
      .replace(/class="([\w-\s]+)"/g, (match, classNames: string) => {
        return `class="${replaceDynamicNames(classNames, classNameMap, classNameChecks)}"`
      })
      /** Cycle through IDs and replace matching patterns */
      .replace(/id="([\w-\s]+)"/g, (match, idNames: string) => {
        return `id="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/name="([\w-\s]+)"/g, (match, idNames: string) => {
        return `name="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/for="([\w-\s]+)"/g, (match, idNames: string) => {
        return `for="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/aria-describedby="([\w-\s]+)"/g, (match, idNames: string) => {
        return `aria-describedby="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/aria-labelledby="([\w-\s]+)"/g, (match, idNames: string) => {
        return `aria-labelledby="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/aria-controls="([\w-\s]+)"/g, (match, idNames: string) => {
        return `aria-controls="${replaceDynamicNames(idNames, idMap, idChecks)}"`
      })
      .replace(/src="([a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*)"/g, () => {
        return `src="snapshotSrcPathPlaceholder"`
      })
  )
}

/**
 * Helper for replacing all dynamically generated names in string for passed checks
 */
const replaceDynamicNames = (names: string, nameMap: Record<string, string>, patternChecks: IPatternCheckItem[]) =>
  names
    .split(/\s+/)
    .map((name) => {
      /** First, check if this is a name we've already generated a replacement for */
      if (Object.keys(nameMap).includes(name)) {
        return nameMap[name]
      }
      /** Loop through our defined pattern checks and replace if match */
      for (let idx = 0; idx < patternChecks.length; idx++) {
        const match = patternChecks[idx].match
        if (typeof match === "string") {
          /** Process string comparison */
          if (name.startsWith(match)) {
            nameMap[name] = patternChecks[idx].prefix
              ? `${patternChecks[idx].prefix}_${patternChecks[idx].matchCount++}`
              : `${match}${patternChecks[idx].matchCount++}`
            return nameMap[name]
          }
        } else {
          /** Process RegExp comparison */
          const searchPattern = new RegExp(match)
          if (searchPattern.test(name)) {
            nameMap[name] = patternChecks[idx].prefix
              ? `${patternChecks[idx].prefix}_${patternChecks[idx].matchCount++}`
              : `c_${patternChecks[idx].matchCount++}`
            return nameMap[name]
          }
        }
      }
      /** return original name if no pattern match */
      return name
    })
    .join(" ")

/**
 * Helper for removing all Mantine <style> tags from passed markup
 */
const removeMantineTags = (markup: string) => {
  let strippedMarkup = markup
  while (strippedMarkup.includes("<style data-mantine-styles")) {
    const mantineStart = strippedMarkup.indexOf("<style data-mantine-styles")
    const mantineEnd = strippedMarkup.indexOf("</style>", mantineStart)
    strippedMarkup = strippedMarkup.substring(0, mantineStart) + strippedMarkup.substring(mantineEnd + "</style>".length)
  }
  while (strippedMarkup.includes("script data-mantine-script")) {
    const mantineStart = strippedMarkup.indexOf("<script data-mantine-script")
    const mantineEnd = strippedMarkup.indexOf("</script>", mantineStart)
    strippedMarkup = strippedMarkup.substring(0, mantineStart) + strippedMarkup.substring(mantineEnd + "</script>".length)
  }
  return strippedMarkup
}
