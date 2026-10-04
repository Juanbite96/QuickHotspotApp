package com.dev.quickhotspot

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class HotspotTileService : TileService() {

    private val isMobileDataOn: Boolean
        get() = Settings.Global.getInt(contentResolver, "mobile_data", 0) == 1

    override fun onClick() {
        super.onClick()

        try {
            // 1. Kiểm tra & Bật Mobile Data ngầm nếu chưa bật
            if (!isMobileDataOn) {
                setMobileData(true)
            }

            // 2. Kích hoạt Hotspot
            enableHotspot()

            // 3. Cập nhật UI nút Cài đặt nhanh
            updateTile(Tile.STATE_ACTIVE)

        } catch (e: Exception) {
            e.printStackTrace()
            updateTile(Tile.STATE_INACTIVE)
        }
    }

    private fun setMobileData(enabled: Boolean) {
        try {
            Settings.Global.putInt(contentResolver, "mobile_data", if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableHotspot() {
        val context = applicationContext
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Thử kích hoạt qua System Method ẩn
        var success = false
        try {
            val methods = cm.javaClass.declaredMethods
            for (method in methods) {
                if (method.name == "startTethering") {
                    method.isAccessible = true
                    // Dùng tham số chuẩn của Android System: type 0 (TETHERING_WIFI), showProvisioningIfPossible false
                    method.invoke(cm, 0, false, null)
                    success = true
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Nếu OS chặn lệnh gọi ngầm hoàn toàn, mở nhanh trang Cài đặt Tethering để chạm bật 1-click
        if (!success) {
            val intent = Intent().apply {
                action = "android.intent.action.MAIN"
                setClassName("com.android.settings", "com.android.settings.TetherSettings")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivityAndCollapse(intent)
            } catch (e: Exception) {
                // Fallback chuẩn Android
                val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivityAndCollapse(fallbackIntent)
            }
        }
    }

    private fun updateTile(state: Int) {
        qsTile?.apply {
            this.state = state
            updateTile()
        }
    }
}