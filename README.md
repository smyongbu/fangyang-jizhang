# 方羊记账

一个简洁的个人记账 Android App。原生 Kotlin + Jetpack Compose + Room，数据保存在手机本地。

## 功能（v1）

- 记一笔：收入 / 支出、金额、分类（带 emoji 图标）、备注、日期
- 首页：按月切换，顶部显示当月收入 / 支出 / 结余汇总
- 账单列表：按天分组，显示每天净额
- 点账单可编辑或删除
- 本地数据库存储（Room），关机重启数据都在

## 怎么跑起来

1. 安装 **Android Studio**（官网：https://developer.android.com/studio ，下载后一路默认安装，它会自带 Android SDK 和模拟器）。
2. 打开 Android Studio → **Open** → 选这个文件夹（方羊记账）。
3. 第一次打开会自动联网下载依赖（Gradle Sync），等进度条跑完。
4. 顶部选一个模拟器（没有就点 Device Manager 新建一个），或用数据线连真机（需打开「开发者选项 → USB 调试」）。
5. 点绿色 ▶ 运行。

## 代码结构

```
app/src/main/java/com/fangyang/jizhang/
├─ FangYangApp.kt          全局 Application，持有数据库 / 仓库
├─ MainActivity.kt         入口 Activity
├─ data/                   数据层
│  ├─ Transaction.kt       账单实体（Room）
│  ├─ TransactionType.kt   收入 / 支出枚举
│  ├─ Category.kt          预置分类
│  ├─ TransactionDao.kt    数据库操作
│  ├─ AppDatabase.kt       Room 数据库
│  ├─ Converters.kt        枚举存取转换
│  └─ TransactionRepository.kt  仓库（以后接云同步在这层）
├─ ui/                     界面层
│  ├─ TransactionViewModel.kt   状态与逻辑
│  ├─ AppNav.kt            页面导航
│  ├─ HomeScreen.kt        首页（汇总 + 账单列表）
│  ├─ AddEditScreen.kt     记一笔 / 编辑
│  └─ theme/               配色、主题
└─ util/Format.kt          金额、日期格式化
```

## 后续可以加

- 分类统计 / 图表（饼图、趋势）
- 自定义分类
- 预算、账户
- 云同步、多设备（在 `TransactionRepository` 这一层接入后端）
- 导出 Excel / CSV
