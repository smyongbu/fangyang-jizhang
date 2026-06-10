package com.fangyang.jizhang

import android.app.Application
import com.fangyang.jizhang.data.AppDatabase
import com.fangyang.jizhang.data.ProductRepository
import com.fangyang.jizhang.data.sync.SyncManager

/**
 * 全局 Application。数据库、仓库、同步管理器是单例，挂在这里，
 * ViewModel / Worker 通过它拿到。
 */
class FangYangApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { ProductRepository(database.productDao()) }
    val syncManager by lazy { SyncManager(this, repository) }
}
