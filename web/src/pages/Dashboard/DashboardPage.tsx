import { FC } from "react"
import { useLogout } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import OrderForm from "./forms/OrderForm"

/**
 * Application Dashboard page
 */
const DashboardPage: FC = () => {
  const logoutMutation = useLogout()
  const { currentUser, setCurrentUser } = useGlobalStore()

  /**
   * User logout handler
   */
  const handleLogout = async () => {
    try {
      await logoutMutation.mutateAsync()
      setCurrentUser({ username: "" })
    } catch {
      alert("Logout failed")
    }
  }

  /**
   * Handle manual order submission
   * @param orderData
   */
  const handleOrder = (orderData: { symbol: string; quantity: number }) => {
    console.log("Submitting order:", orderData)
    // TODO: call backend API
  }

  return (
    <div style={{ padding: 20 }}>
      <h1>Welcome, {currentUser.username}</h1>
      <OrderForm onSubmit={handleOrder} />
      <button onClick={handleLogout}>Logout</button>
    </div>
  )
}

export default DashboardPage
