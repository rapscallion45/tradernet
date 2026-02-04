import { RegisterOptions } from "react-hook-form"
import { PasswordSettings } from "api/types"

export const getPasswordValidationRules =
  ({
    alphasAndNumericsEnabled,
    maxLength,
    minLength,
    repetitionThreshold,
    startsWithAlphaEnabled,
    upperAndLowerAlphasEnabled,
  }: PasswordSettings): RegisterOptions["validate"] =>
  (value: string) => {
    if (value) {
      //Check length first, as least expensive
      if (value.length < minLength) return `Must be at least ${minLength} characters`
      if (value.length > maxLength) return `Must be ${maxLength} characters or fewer`

      //If we need to enforce that it contains upper and lower case characters
      if (upperAndLowerAlphasEnabled) {
        const chars = value.split("")
        const containsLowerCase = chars.some((c) => c !== c.toUpperCase())
        const containsUpperCase = chars.some((c) => c !== c.toLowerCase())
        if (!containsLowerCase || !containsUpperCase) return "Must contain both uppercase and lowercase characters"
      }

      //If we need to enforce that it contains alphabetical and numeric characters
      if (alphasAndNumericsEnabled) {
        if (!/[0-9]+/.test(value)) return "Must contain at least one number"
        if (!/[a-zA-Z]+/.test(value)) return "Must contain at least one alphabetic character"
      }

      //If we need to enforce that it starts with an alphabetical character
      if (startsWithAlphaEnabled) {
        if (!/^[a-zA-Z]/.test(value)) return "Must start with an alphabetic character"
      }

      //If we need to enforce that characters don't occur more than the limit
      if (repetitionThreshold > 0 && charOccursMoreThanLimit(value, repetitionThreshold)) {
        return `Must not contain the same character more than ${repetitionThreshold} time(s)`
      }
    }
    return true
  }

/**
 * Checks if any character in the string occurs more times than a limit
 *
 * @param stringOfChars The string of characters to check
 * @param limit The maximum allowed occurrences of any character
 */
const charOccursMoreThanLimit = (stringOfChars: string, limit: number): boolean => {
  const charCount: Record<string, number> = {}
  for (const char of stringOfChars) {
    const newCount = (charCount[char] ?? 0) + 1
    if (newCount > limit) return true
    charCount[char] = newCount
  }
  return false
}
