import { FieldValues, Path, UseFormGetValues } from "react-hook-form"

/**
 * Validates that the field matches the value of another field.
 *
 * This is a higher-order function that returns a function that can be used as a validation rule.
 *
 * @param fieldName The name of the field to match
 * @param getValues The function to get the values of the form
 */
export function validateFieldMatches<T extends FieldValues>(fieldName: Path<T>, getValues: UseFormGetValues<T>): (value: string) => true | string {
  return (value: string) => {
    const otherValue = getValues(fieldName)
    if (value !== otherValue) {
      return `Must match ${fieldName} value`
    }
    return true
  }
}
