package com.fangyang.jizhang

import android.app.Application
import com.fangyang.jizhang.data.AppDatabase
import com.fangyang.jizhang.data.TransactionRepository

/**
 * 全局 Application。数据库和仓库是单例，挂在这里，
 * ViewModel 通过它拿到 repository。
 */
class FangYangApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }
}
