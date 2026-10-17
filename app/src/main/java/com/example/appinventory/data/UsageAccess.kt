package com.example.appinventory.data

import android.app.AppOpsManager
import android.content.Context

object UsageAccess {
    fun hasUsageAccess(ctx: Context): Boolean {
        val aom = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, ctx.applicationInfo.uid, ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
