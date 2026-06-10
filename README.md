# 扫码记账

一个给小店用的**扫码进货记录** Android app。原生 Kotlin + Jetpack Compose + Room + CameraX + ML Kit，数据保存在手机本地。

## 功能

底部两个标签页：

**① 记录**
- 进入即摄像头取景，自动识别**一维条形码**（EAN-13/UPC 等商品码，不扫二维码）
- 识别后进入填写界面：
  - 条形码若未绑定商品名则手动填写（一个条形码绑定一个名字），已绑定则自动填入
  - 进货日期（默认今天）、生产日期、过期日期，统一年月日；点击后先选年（今年±3 共 7 个）→ 月 → 日
  - 进货价格、零售价格，数字键盘输入，带 ¥
  - 点「确定」保存，回到扫码界面继续扫下一个

**② 查询**
- 按记录时间排序，最新的在最下面，进入自动滚到底部

## 怎么得到 APK

本项目用 GitHub Actions 云端编译，本地无需安装任何环境：

1. push 代码到 GitHub
2. 仓库 **Actions** 页 → 最新一次运行 → 底部 **Artifacts** 下载 `app-debug-apk`
3. 把 APK 传到安卓手机，点开安装（需允许「未知来源」）

## 代码结构

```
app/src/main/java/com/fangyang/jizhang/
├─ FangYangApp.kt          全局 Application，持有数据库 / 仓库
├─ MainActivity.kt         入口
├─ data/                   数据层
│  ├─ ProductRecord.kt     进货记录实体
│  ├─ BarcodeBinding.kt    条形码↔商品名 绑定
│  ├─ ProductDao.kt / AppDatabase.kt / ProductRepository.kt
├─ ui/
│  ├─ ProductViewModel.kt  状态与逻辑
│  ├─ MainScaffold.kt      底部两个标签页 + 导航
│  ├─ scan/                扫码（CameraX + ML Kit）
│  ├─ record/              填写界面 + 级联年月日选择
│  └─ query/               查询列表
└─ util/Format.kt          金额、日期格式化
```

## 后续可以加

- 按商品名/条形码搜索、按过期日期提醒
- 库存数量、出入库
- 自定义数字键盘、编辑/删除记录
- 导出 Excel、云同步
