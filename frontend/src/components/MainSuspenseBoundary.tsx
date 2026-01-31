import { FC } from "react"
import PageLoadingSpinner from "components/PageLoadingSpinner"

/**
 * Highest suspense boundary in the application as a last fallback
 */
const MainSuspenseBoundary: FC = () => <PageLoadingSpinner message={"Initialising..."} />

export default MainSuspenseBoundary
