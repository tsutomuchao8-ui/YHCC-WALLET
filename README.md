# YHCC WALLET — Android 项目

黑金风格 Android 资产展示应用，使用 Kotlin + Jetpack Compose。

## 当前版本包含
- 首页资产总览
- BTC / ETH / USDT 初始条目
- 添加、编辑、删除资产
- 自定义持有数量和 USD 单价
- 行情列表（使用本机维护的单价）
- 收款、转账界面预览
- 交易记录列表
- 个人中心

## 重要边界
此版本是本机资产展示工具，不连接区块链、不生成或保存私钥/助记词，不广播或执行真实转账。转账界面只用于填写信息预览。示例资产与记录是应用初始展示内容，用户可以自行修改。

## 用 Android Studio 构建 APK
1. 在 Android Studio 中选择 Open，打开本项目文件夹。
2. 等待 Gradle Sync 完成，并安装 Android SDK Platform 35。
3. 选择 Build > Build Bundle(s) / APK(s) > Build APK(s)。
4. APK 通常位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 环境
- minSdk 26
- compileSdk 35
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
