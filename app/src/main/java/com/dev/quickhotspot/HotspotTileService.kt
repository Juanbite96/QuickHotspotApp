package com.dev.quickhotspot

import android.content.Context
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import java.util.concurrent.Executors

class HotspotTileService : TileService() {

    private val isMobileDataOn: Boolean
        get() = Settings.Global.getInt(contentResolver, "mobile_data", 0) == 1

    override fun onClick() {
        super.onClick()

        try {
            // 1. Nếu Mobile Data đang tắt -> Bật lên
            if (!isMobileDataOn) {
                setMobileData(true)
            }

            // 2. Bật Hotspot
            enableHotspot()

            // 3. Cập nhật UI nút trên One UI
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
        val tm = getSystemService(Context.TETHERING_SERVICE) as? TetheringManager
        if (tm != null) {
            val request = TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI)
                .setShouldShowEntitlementUi(false)
                .build()

            tm.startTethering(
                request,
                Executors.newSingleThreadExecutor(),
                object : TetheringManager.StartTetheringCallback {
                    override fun onTetheringFailed(error: Int) {
                        updateTile(Tile.STATE_INACTIVE)
                    }
                }
            )
        } else {
            // Chạy fallback qua lệnh shell hệ thống
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