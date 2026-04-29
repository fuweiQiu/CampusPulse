# CampusPulse

- 隊伍名稱：老師叫我來試試
- 作品名稱：CampusPulse
- 主題領域：醫療資訊、校園心理健康、數位健康追蹤、學生福祉與早期風險辨識
- 使用者角色：病人(對象：學生使用者)
- 核心 FHIR Resources：Patient、Observation、Bundle
- Demo 入口：https://youtu.be/sN1yNAFnK8Y
- 如何執行：如下方文件記載

CampusPulse 是一個示範性學生健康平台，使用 Spring Boot 3.x 提供 REST API，React 18 + Tailwind CSS 提供單頁前端。新版重點是把健康紀錄重構成更接近 FHIR 的資料形狀，並把單次打卡改成多輪聊天式收集流程，同時支援把 FHIR `Patient` / `Observation` 同步到雲端 HAPI FHIR public test server。

## 專案結構

```text
CampusPulse/
├─ backend/
│  └─ src/main/java/com/campuspulse/
│     ├─ controller/   # API 入口
│     ├─ service/      # FHIR resource 建立、多輪聊天、聚合運算
│     ├─ repository/   # JPA 資料存取
│     ├─ model/        # User / ObservationRecord / ChatSession / ChatMessage / CommunityPost
│     ├─ security/     # JWT 產生與驗證
│     └─ config/       # Security / CORS / FHIR Context
└─ frontend/
   └─ src/
      ├─ pages/        # Auth / Chat / Dashboard / Community
      ├─ components/   # 版型、卡片與圖表包裝
      ├─ context/      # 登入狀態
      ├─ api/          # Axios client
      └─ utils/        # 格式化工具
├─ python-fhir/
│  ├─ create_patient.py
│  ├─ create_temperature_observation.py
│  └─ fetch_temperature_observations.py
```

## 後端技術

- Spring Boot 3.5.12
- Spring MVC
- Spring Data JPA
- Spring Security + BCrypt
- JWT (`jjwt`)
- HAPI FHIR R4
- H2 Database
- Java `HttpClient` 直連雲端 HAPI FHIR server

## 前端技術

- React 18
- Vite
- Tailwind CSS 4
- Axios
- React Router
- Recharts

## API 摘要

- `POST /api/register`：註冊並回傳 token
- `POST /api/login`：登入並回傳 token
- `POST /api/chat`：聊天式送出訊息，若欄位不足會回覆追問
- `GET /api/chat/history?token=...`：取得聊天紀錄與最近一次 Observation
- `GET /api/metrics?token=...`：讀取近 7 天平均壓力、睡眠與情緒分佈
- `POST /api/community/posts`：發布匿名貼文
- `GET /api/community/posts`：列出最新匿名貼文
- `GET /api/fhir/patient?token=...`：輸出 FHIR Patient JSON
- `GET /api/fhir/observations?token=...`：輸出 Observation JSON 清單
- `GET /api/fhir/bundle?token=...`：輸出 Patient + Observation 的 FHIR Bundle

## 雲端 FHIR 同步

後端預設會把使用者對應的 `Patient` 與聊天完成後建立的 `Observation`，同步到：

- `https://hapi.fhir.org/baseR4`

可在 `backend/src/main/resources/application.yml` 調整：

```yaml
app:
  fhir:
    remote:
      enabled: true
      fail-on-sync-error: false
      base-url: https://hapi.fhir.org/baseR4
```

說明：

- `enabled=true`：啟用雲端同步
- `fail-on-sync-error=false`：若 public server 暫時不可用，系統仍保留本地 FHIR JSON，不會直接中斷註冊或聊天流程
- 註冊 / 登入成功後，前端頁首會顯示雲端 `Patient` 連結

## Python for FHIR 規則範例

決賽要求的 Python 範例放在 `python-fhir/`，可直接獨立執行：

```bash
cd python-fhir
python3 create_patient.py
python3 create_temperature_observation.py 52960712 --temperature 38
python3 fetch_temperature_observations.py 52960712
```

三支程式分別對應：

- 新增病人
- 新增病人體溫 Observation
- 調閱並呈現病人體溫資料

## 啟動方式

### 後端

Spring Boot 3 需要 JDK 17 或 21。本機若沒有 `mvn`，可直接使用 Maven Wrapper。

```bash
cd backend
./mvnw spring-boot:run
```

若你本機已安裝 Maven，也可以使用：

```bash
cd backend
mvn spring-boot:run
```

後端預設在 `http://localhost:8080` 啟動，H2 Console 為 `http://localhost:8080/h2-console`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端預設在 `http://localhost:5173` 啟動。

## 備註

- JWT 會在登入或註冊後簽發，並儲存在 `User.authToken` 作為簡化版單一登入 token。
- 使用者會同步建立對應的 FHIR `Patient`；聊天完成後會建立 `Observation`，並把完整 `resourceJson` 儲存到資料庫。
- 若網路可用，`Patient` / `Observation` 也會同步上傳到雲端 HAPI FHIR server，並記錄資源 URL。
- 健康紀錄改成多輪聊天：若缺少壓力、睡眠或情緒，機器人會追問，不再捏造睡眠時數。
- 若使用者回覆「不知道」，系統會在 FHIR component 使用 `dataAbsentReason=unknown`，而 dashboard 平均值不採計空值。
- 目前情緒與壓力的 code system 仍有 CampusPulse 自訂碼，結構上接近 FHIR，但還不是可直接上醫療院所正式互通的法規級實作。
- 目前的聊天機器人是規則式對話，不依賴外部付費 AI 服務；優點是免費、可控、可離線開發，缺點是語意理解仍有限。
- `hapi.fhir.org` 是公開測試伺服器，請不要存放真實病歷或個資，且資料可能被定期清除。
