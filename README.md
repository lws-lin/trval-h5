# trval-h5

旅游规划 H5。用户用自己的 DeepSeek API Key 登录，通过表单或对话收集出发地、目的地、预算和天数，由大模型生成行程。行程记录保存在浏览器本机，服务端不存用户数据。

## 功能

- **登录**：填写用户名、DeepSeek API Key，并选择模型（DeepSeek Flash 或 DeepSeek V4 Pro）。Key 和模型只存在当前浏览器。
- **首页**：选择出发城市、目的地、预算和天数，直接生成行程。
- **对话**：流式回复。助手只处理旅游规划，并引导补齐出发地、目的地、预算、天数和其他需求。已确认的信息显示在卡片上，凑齐后可以生成行程。
- **行程详情**：展示日程。从对话进入时使用对话里已经生成的行程，不再请求一次大模型。
- **我的**：按用户名查看保存在本机的行程。同一段对话再次保存会覆盖原记录。可以从记录继续对话。退出登录不会删除这些记录。

## 技术栈

- 前端：Vue 3、Vite、Vant、Vue Router
- 后端：Java 17、Spring Boot 3.4.5
- 模型：DeepSeek，`https://api.deepseek.com`
- 部署：Nginx 托管页面并转发 `/api`，后端跑在容器内网

## 目录

```text
trval-h5/
├── deploy.sh                 构建并启动 / 停止 / 查看日志
├── docker-compose.yml        web 与 server 两个服务
├── server/
│   ├── Dockerfile            编译并运行后端
│   ├── pom.xml
│   └── src/main/java/com/trval/
│       ├── chat/             POST /api/chat，流式对话
│       ├── plan/             POST /api/plan，生成行程
│       ├── llm/              调用 DeepSeek
│       └── config/           跨域配置
└── web/
    ├── Dockerfile            编译前端，用 Nginx 提供页面
    ├── nginx.conf            页面路由，以及 /api 反代
    ├── .env.development      本地开发的接口前缀和代理地址
    ├── .env.production       构建进前端包的接口前缀
    └── src/
        ├── views/            登录、首页、对话、详情、我的
        ├── api/              对话与行程请求
        ├── components/chat/  已确认信息、建议和生成按钮
        └── utils/            登录态、对话草稿、本机行程、接口地址
```

对话草稿在 `sessionStorage`，键名是 `trval-chat-draft:用户名`。已保存的行程在 `localStorage` 的 `trval-plans` 里，按用户名分开。

## 本地开发

需要 Java 17、Maven 和 Node.js。先启动后端：

```bash
cd server
mvn spring-boot:run
```

后端监听 `8080`。再启动前端：

```bash
cd web
npm install
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。开发时前端请求 `/api`，由 Vite 转发到 `web/.env.development` 里的 `API_PROXY_TARGET`（默认 `http://127.0.0.1:8080`）。

## Docker 部署

安装并启动 Docker 后，在项目根目录执行：

```bash
./deploy.sh
```

浏览器打开 `http://127.0.0.1`。手机和电脑在同一 Wi-Fi 时，用电脑的局域网 IP 打开，例如 `http://192.168.x.x`。

```bash
./deploy.sh logs    # 查看日志
./deploy.sh down    # 停止并删除容器
PORT=8088 ./deploy.sh
```

`PORT` 是宿主机对外端口，容器内部仍由 Nginx 监听 80。默认从 `docker.m.daocloud.io` 拉取基础镜像，这是 DaoCloud 提供的 Docker Hub 公共镜像站。能直接访问 Docker Hub 时：

```bash
IMAGE_PREFIX=docker.io/library ./deploy.sh
```

Nginx 把 `/api` 转到名为 `server` 的后端容器。后端端口 `8080` 不映射到宿主机。运行这套容器的机器需要能访问 `https://api.deepseek.com`。

## 云服务器

前后端仍放在同一台机器、由 Nginx 统一对外时，不用改接口地址。把项目拷到服务器后执行 `./deploy.sh`，安全组放行对外端口（默认 80）。不要把容器内的 `8080` 暴露到公网。

页面和接口拆到不同域名时，改两处后重新构建：

- `web/.env.production` 的 `VITE_API_BASE_URL`，写成后端完整地址，例如 `https://api.example.com/api`。这个值在构建时写进前端。
- `server/src/main/resources/application.yml` 的 `app.cors.allowed-origin-patterns`，加上前端页面的域名。

同一域名、只在前面加 HTTPS 时，保持 `VITE_API_BASE_URL=/api` 即可。
