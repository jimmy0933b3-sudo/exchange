# Exchange｜Google 登入與 Drive 私有備份設定

本功能使用 **Credential Manager + Google Sign-In** 作身份登入，並使用 **Google Play services AuthorizationClient** 獨立取得 `drive.appdata` 權限。沒有外部後端，也不需 Firebase；不存取整個雲端硬碟，亦不會儲存 OAuth access/refresh token。

## 初次設定 Google Cloud

1. 建立 Google Cloud 專案，啟用 **Google Drive API**。
2. 在 Google Auth Platform → Branding / Audience 完成 OAuth 同意畫面，測試階段將要使用的 Google 帳號加入 Test users。依 Google 對 scopes 的現行要求，選擇 `https://www.googleapis.com/auth/drive.appdata`。
3. 建立 **Android OAuth Client**，Package Name 使用 `com.fubon.fxmonitor`。簽章 SHA-1 需使用 GitHub Actions 目前正式 Release APK 的簽章金鑰，**不可使用本機 Debug Key SHA-1**。
   - 可在 Actions → Sign and publish latest release → Build and verify signed Release APK 內找到 `apksigner verify --print-certs` 的 `certificate SHA-1 digest`。
   - 若使用相同正式簽章金鑰，此 SHA-1 必須保持固定。
4. 同一個 Google Cloud 專案再建立 **Web application 類型 OAuth Client ID**，供 Credential Manager `GetSignInWithGoogleOption` 的 `serverClientId` 使用。不要填 Android Client ID。
5. 在 GitHub Repo → Settings → Secrets and variables → Actions → New repository secret 設定 `GOOGLE_WEB_CLIENT_ID`（完整 `...apps.googleusercontent.com` 字串）。
6. 在 GitHub Actions → Android CI and Signed Release → Run workflow → master 建置新 APK。只有重新建置後，ID 才會包含在 BuildConfig。此 ID 本身不是秘密，GitHub Secrets 僅用來管理建置參數。
7. 安裝最新版正式簽章 APK，至「偏好設定 → Google 帳戶與雲端備份」，先登入，再授權 Drive `appDataFolder`。

## 功能說明

- 不登入仍可使用匯率、試算、提醒與 Widget。
- 可手動備份、查看私有備份清單、選擇一筆備份合併還原。
- 每天使用 WorkManager 週期工作，預設只在 Wi-Fi / 非計量網路上傳；Android 可能延後工作，不保證固定時刻。
- 自動保留最新 7 筆備份；成功上傳並通過雜湊比對後才清理較舊資料。
- 還原前比對 Drive 檔案雜湊及壓縮檔格式，匯率歷史依幣別與官方牌告時間去重。
- 提醒條件新增時重新配發本機 ID，避免不同手機的 ID 撞號；既有本機資料不刪除。
- 會還原主要設定；Widget 的 Android 系統 ID 是裝置專用，因此不會直接覆蓋新手機 Widget。
- 使用者登出後停止背景備份；雲端檔案不會隨登入狀態切換而刪除。
- 本版是 **雲端備份 / 合併還原**，不是即時雙向同步。多部手機同時使用時，以版本選擇還原；請勿將其當作同時編輯的即時同步資料庫。

## 維護與診斷

- 未設定 `GOOGLE_WEB_CLIENT_ID` 的 APK 會保留基本匯率功能，但顯示 OAuth 未配置提醒。
- 授權過期或被撤銷後，背景 Worker 不會彈出介面；到 App 手動重新授權。
- 使用者切換 Google 帳號時，自動備份會停止，需重新授權新帳號，以免備份到錯誤帳號。
- Google Drive `appDataFolder` 是不可直接從 Drive UI 看見的隱藏應用程式資料區，通常也無法手動分享或移到其他資料夾。
- Google OAuth 授權與雲端真實存取無法單靠 GitHub Actions 的一般單元測試端到端驗證，仍需以啟用 Drive API 的實際 Google 帳號在真機測試。
