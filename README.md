# 我的匯率 (台北富邦銀行匯率監控)

[![Android CI Build](https://github.com/jimmy0933b3-sudo/exchange/actions/workflows/android.yml/badge.svg)](https://github.com/jimmy0933b3-sudo/exchange/actions)

「我的匯率」是一款專為台北富邦銀行匯率監控設計的原生 Android App。不需要登入銀行帳號即可自動查詢最新公開牌告匯率、多外幣即期與現鈔監控、自訂目標匯率到價通知、歷史趨勢圖表、桌面 Widget 與換匯試算。

---

## 📱 主要功能說明

1. **公開匯率監控與自動解析**：
   - 優先使用台北富邦銀行官方網站，解析 14 種官方幣別（USD、JPY、CNY、EUR、HKD、AUD、CAD、GBP、SGD、CHF、NZD、SEK、THB）。
   - 支援解析官方牌告更新時間與資料新鮮度狀態（即時、快取、資料過期警示）。
   - 當部分幣別無現鈔報價（如 ZAR、GBP、SGD、CHF、NZD、SEK、THB）時正確標示為 N/A，不自動補零。

2. **多幣別收藏與首頁動態卡片**：
   - 預設收藏：美元 USD、日圓 JPY、人民幣 CNY、歐元 EUR。
   - 支援即期 / 現鈔切換、手動下拉更新、深色模式與離線快取顯示。

3. **匯率到價通知（核心功能）**：
   - 支援 4 種提醒條件：大於等於指定價格、小於等於指定價格、單次漲跌百分比、每日漲跌百分比。
   - Android 原生系統通知，具備防重複發送機制（離開門檻後重新進入方再次觸發）與完整觸發紀錄。

4. **背景監控與啟動復原**：
   - WorkManager 處理背景排程（預設 15 分鐘輪詢與退避重試政策）。
   - `BootReceiver` (RECEIVE_BOOT_COMPLETED) 在手機重啟後自動恢復背景監控作業。

5. **歷史匯率與趨勢圖表**：
   - Canvas 自訂繪製趨勢圖，支援 今天、7天、30天、3個月、6個月 時間區間。
   - 顯示區間最高價、最低價、平均價與漲跌幅度。歷史紀錄保存於 Room Database。

6. **Android 桌面 Widget**：
   - 小型 Widget（單一指定幣別）、中型 Widget（精選 3 種外幣）、大型 Widget（多外幣看板）。
   - 採用 AndroidX Glance 開發，共用 Room 數據快取與點擊開 App。

7. **換匯計算機**：
   - 支援雙向台幣 ⇆ 外幣試算，使用 `BigDecimal` 避免浮點數誤差，精確套用銀行買入/賣出價。

---

## ☁️ Google 帳戶與雲端備份

設定 → **Google 帳戶與雲端備份**：可選擇使用 Google 帳號登入、授權 `drive.appdata` 後手動備份、查看歷史版本、合併還原，或啟用每天一次的 WorkManager 自動備份（預設僅 Wi-Fi）。保留最近 7 筆備份，登出不會清除雲端資料；未登入仍可使用匯率功能。

**初次啟用需要 Google Cloud OAuth 專案配置及 Android 發行簽章 SHA-1，且必須將 Web OAuth Client ID 設為 GitHub Actions `GOOGLE_WEB_CLIENT_ID` 後重新打包。** 沒有這項配置時，備份頁會顯示提示，不會嘗試使用無效 Client ID 登入。

請先看 [Google 登入與雲端備份設定指南](CLOUD_BACKUP_SETUP.md)。

---

## 🔑 CI / CD 說明

GitHub Actions 在 PR 與開發分支執行單元測試及 Debug APK 建置。**只有 master 通過測試後**，才使用固定簽章金鑰建置可更新安裝的正式 Release APK。

請在本 repository 的 **Settings → Secrets and variables → Actions → Repository secrets** 設定：

| Secret | 內容 |
| --- | --- |
| `KEYSTORE_BASE64` | 固定 Android release keystore (.jks / .p12) 的 Base64 內容 |
| `KEYSTORE_PASSWORD` | Keystore 密碼 |
| `KEY_ALIAS` | 用於簽署 APK 的 alias |
| `KEY_PASSWORD` | 簽章私鑰密碼 |

使用與其他 Android 專案相同的 Secret 名稱，但 **Secrets 是逐個 repository 設定**，不會從別的 repository 自動繼承。切勿將 keystore、密碼或 Base64 私鑰提交至 Git。若 Secret 不完整、簽章無效或與先前 Release 的憑證不同，Release 工作會失敗，保留原本的版本，不退回 Debug 簽章。

- 每次 master CI 成功即建立新的正式簽章 Release，包含 `exchange-latest.apk`、`exchange-latest.apk.sha256` 及 `SIGNING-INFO.txt`。
- 新 Release 成功發布且確認檔案齊全後，才會刪除所有舊 Releases 與其標籤。清理失敗會使工作失敗，請手動檢查。
- `versionCode` 依 GitHub Actions run number 遞增，Release 簽章金鑰固定，讓新 APK 能覆蓋安裝舊版（同一 applicationId 與簽章前提）。
- 下載固定入口：[最新 Release](https://github.com/jimmy0933b3-sudo/exchange/releases/latest)。
- **Fork 專案首次使用 CI**：若 Actions 顯示 0 筆執行紀錄，先在 [Actions](https://github.com/jimmy0933b3-sudo/exchange/actions) 啟用工作流程，或到 [Settings → Actions → General](https://github.com/jimmy0933b3-sudo/exchange/settings/actions) 確認允許執行；再於 Actions → Android CI and Signed Release → Run workflow 選取 `master` 手動執行。尚未啟用的工作流程，不會因為新 commit 而自動發布 APK。
- 本專案原本的 Debug APK 使用 Android Debug Keystore，與 Release 不同，不保證能直接覆蓋安裝；若曾安裝 Debug 版，首次改裝 Release 版可能需要先解除安裝。


---

## 🛠️ 技術架構

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose, Material 3
- **Architecture**: MVVM, Repository Pattern, Clean Architecture
- **Asynchronous**: Coroutines, Flow
- **Dependency Injection**: Hilt
- **Local Storage**: Room Database, DataStore Preferences
- **Background Work**: WorkManager, BroadcastReceiver
- **Widgets**: AndroidX Glance AppWidget
- **Network / Parsing**: OkHttp, Jsoup HTML Scraper

---

## 🧪 測試與驗證結果

專案包含完整的單元測試，執行命令：
```bash
./gradlew testDebugUnitTest
```
測試涵蓋：
- 官方 HTML 解析與 14 種幣別報價擷取（含 N/A 處理）
- 換匯計算機 `BigDecimal` 精確度
- 到價提醒觸發規則評估
- 幣別代碼與 Data Mapping

---

## 📦 如何下載 APK

前往 [GitHub Releases 的最新版](https://github.com/jimmy0933b3-sudo/exchange/releases/latest)，下載 `exchange-latest.apk` 並安裝。可用附帶的 `exchange-latest.apk.sha256` 驗證完整性。PR 開發測試版則仍可從 Actions 的 Debug Artifacts 下載。

