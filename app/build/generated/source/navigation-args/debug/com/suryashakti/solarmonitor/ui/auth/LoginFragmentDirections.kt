package com.suryashakti.solarmonitor.ui.auth

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.suryashakti.solarmonitor.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    public fun actionLoginToRegister(): NavDirections =
        ActionOnlyNavDirections(R.id.action_login_to_register)

    public fun actionLoginToForgot(): NavDirections =
        ActionOnlyNavDirections(R.id.action_login_to_forgot)
  }
}
