package com.dev.quickhotspot

import android.content.Context
import android.net.ConnectivityManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class HotspotTileService : TileService() {

    private val isMobileDataOn: Boolean
        get() = Settings.Global.getInt(contentResolver, "mobile_data", 0) == 1

    override fun onClick() {
        super.onClick()

        try {
            // 1. Kiểm tra & Bật Mobile Data nếu đang tắt
            if (!isMobileDataOn) {
                setMobileData(true)
            }

            // 2. Kích hoạt Hotspot
            enableHotspot()

            // 3. Cập nhật trạng thái Active trên Quick Panel One UI
            updateTile(Tile.STATE_ACTIVE)

        } catch (e: Exception) {
            e.printStackTrace()
            updateTile(Tile.STATE_INACTIVE)
        }
    }

    private fun setMobileData(enabled: Boolean) {
        Settings.Global.putInt(contentResolver, "mobile_data", if (enabled) 1 else 0)
    }

    private fun enableHotspot() {
        // Gọi lệnh kích hoạt Tethering trực tiếp thông qua Service Manager của Android System
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val method = cm.javaClass.getDeclaredMethod("startTethering", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback"))
            method.isAccessible = true
            method.invoke(cm, 0, false, null)
        } catch (e: Exception) {
            // Fallback: Kích hoạt bằng Shell Command (Hoạt động hoàn hảo khi đã cấp WRITE_SECURE_SETTINGS / ADB)
            Runtime.getRuntime().exec("cmd tethering start wifi")
        }
    }

    private fun updateTile(state: Int) {
        qsTile?.apply {
            this.state = state
            updateTile()
        }
    }
}