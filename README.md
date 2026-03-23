# CampusPulse

- 隊伍名稱：老師叫我來試試
- 作品名稱：CampusPulse
- 主題領域：醫療資訊、校園心理健康、數位健康追蹤、學生福祉與早期風險辨識
- 使用者角色：病人(對象：學生使用者)
- 核心 FHIR Resources：Patient、Observation、Bundle
- Demo 入口：https://youtu.be/sN1yNAFnK8Y
- 如何執行：如下方文件記載

CampusPulse 是一個示範性學生健康平台，使用 Spring Boot 3.x 提供 REST API，React 18 + Tailwind CSS 提供單頁前端。新版重點是把健康紀錄重構成更接近 FHIR 的資料形狀，並把單次打卡改成多輪聊天式收集流程。

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
```

## 後端技術

- Spring Boot 3.5.12
- Spring MVC
- Spring Data JPA
- Spring Security + BCrypt
- JWT (`jjwt`)
- HAPI FHIR R4
- H2 Database

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
- 健康紀錄改成多輪聊天：若缺少壓力、睡眠或情緒，機器人會追問，不再捏造睡眠時數。
- 若使用者回覆「不知道」，系統會在 FHIR component 使用 `dataAbsentReason=unknown`，而 dashboard 平均值不採計空值。
- 目前情緒與壓力的 code system 仍有 CampusPulse 自訂碼，結構上接近 FHIR，但還不是可直接上醫療院所正式互通的法規級實作。
- 目前的聊天機器人是規則式對話，不依賴外部付費 AI 服務；優點是免費、可控、可離線開發，缺點是語意理解仍有限。
