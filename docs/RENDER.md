# Render 单容器部署

镜像在仓库根目录构建（`dockerContext: .`）。容器里是 Java 17 后端，Vue 生产包打进同一个 jar，浏览器和 `/api` 同域。进程监听 `PORT`。

健康检查：`GET /api/health`，返回 `{"status":"UP"}`。无需登录，也不走演示门禁。

## 公网门禁

`PUBLIC_MODE=true` 时进程拒绝启动，除非同时提供：

| 变量 | 作用 |
|---|---|
| `DEMO_GATE_SECRET` | 至少 16 位。访客先在 `/gate.html` 提交，通过后才发 httpOnly cookie。登录接口在这之前不可用。 |
| `DEMO_PASSWORD` | 至少 10 位，且不能是本地演示口令。启动时写入全部演示账号。 |
| `JWT_SECRET` | 至少 32 位，覆盖仓库里的本地占位值。 |

这三个值只放在 Render 的环境变量里。镜像构建会关掉登录页上的本地账号卡片（`VITE_SHOW_DEMO_ACCOUNTS=false`）。H2 控制台、Swagger、Knife4j 在该镜像里关闭。没有 `DEEPSEEK_API_KEY` 时，智能能力按规则降级。

数据库是容器内临时 H2，只灌 `backend/src/main/resources/db/data.sql` 的演示种子。每次启动重建，不携带本机 `backend/data`、上传文件或日志。

## 内存

面向 512MiB：堆上限 256MiB，Serial GC，Tomcat 最多 20 条线程，连接池 3。这些写在 `Dockerfile` 的 `JAVA_TOOL_OPTIONS` 里。

## 本机构建

```bash
docker build -t intelligent-procurement .
docker run --rm -p 8080:8080 \
  -e DEMO_GATE_SECRET \
  -e DEMO_PASSWORD \
  -e JWT_SECRET \
  intelligent-procurement
```

先打开 `/gate.html`，再进入系统。账号名仍是 zhangsan、lisi、wangwu、zhaoliu、caiwu、admin，口令是你设置的 `DEMO_PASSWORD`。
