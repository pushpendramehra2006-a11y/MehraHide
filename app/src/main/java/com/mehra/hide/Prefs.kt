package com.mehra.hide

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("hide", Context.MODE_PRIVATE)

    var on: Boolean
        get() = sp.getBoolean("on", false)
        set(v) = sp.edit().putBoolean("on", v).apply()

    var pin: String
        get() = sp.getString("pin", "") ?: ""
        set(v) = sp.edit().putString("pin", v).apply()

    // In apps ke icon launcher se chhupenge + notification block honge
    var hiddenApps: Set<String>
        get() = (sp.getStringSet("apps", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("apps", v).apply()

    // In folders ke photo/video gallery se chhupenge
    var hiddenFolders: Set<String>
        get() = (sp.getStringSet("folders", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("folders", v).apply()
}
