package com.xiaoyan.pet.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xiaoyan.pet.R

/**
 * 本地通知 (v1.3.0, 方案 §5; 非 FCM):
 * 双渠道 「养成提醒」(升级/跨幕) + 「系统更新」(新版本, 点击回 APP 内下载)。
 * Android 13+ (API 33) POST_NOTIFICATIONS 运行时权限由设置页「通知开关」引导请求一次;
 * 设备 API 29 (P20 Pro) 无此弹窗, 代码分支兼容; 低配模式不影响通知。
 */
object Notifier {

    const val CH_CARE = "care_reminder"
    const val CH_UPDATE = "sys_update"
    private const val NOTIF_UPDATE = 1001
    private const val NOTIF_LEVELUP = 1002
    private const val NOTIF_ACT = 1003

    fun init(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val care = NotificationChannel(CH_CARE, "养成提醒", NotificationManager.IMPORTANCE_DEFAULT)
        care.description = "小桃升级 / 跨幕提醒"
        val update = NotificationChannel(CH_UPDATE, "系统更新", NotificationManager.IMPORTANCE_DEFAULT)
        update.description = "发现新版本 (点击回 APP 内下载)"
        nm.createNotificationChannel(care)
        nm.createNotificationChannel(update)
    }

    private fun enabled(ctx: Context): Boolean {
        if (!SaveRepository(ctx).notificationsOn()) return false
        return runCatching { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }.getOrDefault(false)
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, com.xiaoyan.pet.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun versionFound(ctx: Context, version: String, note: String) {
        if (!enabled(ctx)) return
        init(ctx)
        val n = NotificationCompat.Builder(ctx, CH_UPDATE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("小桃养成记 · 发现新版本 v$version")
            .setContentText(note.ifBlank { "点击回到 APP 内直接下载" })
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_UPDATE, n) }
    }

    fun leveledUp(ctx: Context, level: Int) {
        if (!enabled(ctx)) return
        init(ctx)
        val n = NotificationCompat.Builder(ctx, CH_CARE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("小桃长大了")
            .setContentText("小桃升到了 Lv.$level")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_LEVELUP, n) }
    }

    fun actCrossed(ctx: Context, level: Int, act: Int) {
        if (!enabled(ctx)) return
        init(ctx)
        val season = when (act) {
            2 -> "夏"
            3 -> "秋"
            4 -> "冬"
            else -> "春"
        }
        val n = NotificationCompat.Builder(ctx, CH_CARE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("进入新的季节")
            .setContentText("小桃跨入了 $season（Lv.$level），有一场新的小屋在等她。")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ACT, n) }
    }
}
