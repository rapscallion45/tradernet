import { RestResource } from "api/RestResource"
import { ApiInterface } from "api/ApiInterface"
import { ForgotPasswordData, Get, LoginData, LoginResponse, LogoutResponse, MessageResponse, Post, SessionInfo } from "api/types"

/**
 * Auth resource definition
 */
export class AuthResource extends RestResource<LoginResponse, LoginData> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "auth")
  }

  login(data: LoginData): Post<LoginResponse> {
    return this.typedSubPath<LoginResponse, LoginData>("login")._create({ data })
  }

  logout(): Post<LogoutResponse> {
    return this.typedSubPath<LogoutResponse, unknown>("logout")._create({ data: {} })
  }

  getSession(): Get<SessionInfo> {
    return this.typedSubPath<SessionInfo>("session")._get()
  }

  forgotPassword(data: ForgotPasswordData): Post<MessageResponse> {
    return this.typedSubPath<MessageResponse, ForgotPasswordData>("forgot-password")._create({ data })
  }
}
