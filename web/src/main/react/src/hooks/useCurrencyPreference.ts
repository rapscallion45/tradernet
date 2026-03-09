import { useLocalStorage } from "@mantine/hooks"
import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"
import { inferCurrencyFromLocale, setUserCurrency } from "utils/intl"

const FALLBACK_CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "INR"]

export const useCurrencyPreference = () => {
  const [currency, setCurrency] = useLocalStorage<string>({
    key: "tradernet.currency",
    defaultValue: inferCurrencyFromLocale(),
    getInitialValueInEffect: false,
  })

  const { data: currencyOptions = FALLBACK_CURRENCIES } = useQuery({
    queryKey: [QueryClientKeys.MarketCurrencies],
    queryFn: async () => {
      const currencies = await getRestClient().marketResource.getCurrencies()
      return currencies.length > 0 ? currencies : FALLBACK_CURRENCIES
    },
    staleTime: 15 * 60 * 1000,
  })

  const updateCurrency = (nextCurrency: string) => {
    setCurrency(nextCurrency)
    setUserCurrency(nextCurrency)
  }

  return {
    currency,
    setCurrency: updateCurrency,
    currencyOptions,
  }
}
