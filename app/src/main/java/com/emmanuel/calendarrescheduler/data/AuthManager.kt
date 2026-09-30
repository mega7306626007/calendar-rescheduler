package com.emmanuel.calendarrescheduler.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthManager {
    const val REQ_PICK_ACCOUNT = 1001
    const val REQ_AUTH = 1002
    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/calendar"

    fun pickAccountIntent(): Intent = AccountManager.newChooseAccountIntent(
        null,
        null,
        arrayOf("com.google"),
        null, null, null, null
    )

    fun getAccount(context: Context): Account? {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val name = prefs.getString("account", null) ?: return null
        return Account(name, "com.google")
    }

    fun saveAccount(context: Context, account: Account) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().putString("account", account.name).apply()
    }

    fun clearAccount(context: Context) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().remove("account").apply()
    }

    suspend fun getToken(context: Context): String = withContext(Dispatchers.IO) {
        val account = getAccount(context) ?: throw IllegalStateException("No account")
        try {
            GoogleAuthUtil.getToken(context, account, SCOPE)
        } catch (e: UserRecoverableAuthException) {
            throw UserRecoverableAuthException(e.message ?: "Auth needed", e.intent)
        }
    }
}