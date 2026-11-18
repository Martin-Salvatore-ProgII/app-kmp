package com.example.turnosapp.auth.ui

sealed interface LoginAction {
    data class UsernameChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data class RememberMeChanged(val value: Boolean) : LoginAction
    data object Submit : LoginAction
    data object RegisterClicked : LoginAction
}

sealed interface LoginEffect {
    data object LoggedIn : LoginEffect
    data object NavigateToRegister : LoginEffect
}
