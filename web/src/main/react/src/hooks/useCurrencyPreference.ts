import { useLocalStorage } from "@mantine/hooks"
import { inferCurrencyFromLocale, setUserCurrency } from "utils/intl"

const COMMON_CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "INR"]

export const useCurrencyPreference = () => {
  const [currency, setCurrency] = useLocalStorage<string>({
    key: "tradernet.currency",
    defaultValue: inferCurrencyFromLocale(),
    getInitialValueInEffect: false,
  })

  const updateCurrency = (nextCurrency: string) => {
    setCurrency(nextCurrency)
    setUserCurrency(nextCurrency)
  }

  return {
    currency,
    setCurrency: updateCurrency,
    currencyOptions: COMMON_CURRENCIES,
  }
}
