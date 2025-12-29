# GitHub Actions 配置说明

## GitHub Secrets 配置

在 GitHub 仓库中配置以下 Secrets（Settings -> Secrets and variables -> Actions -> New repository secret）：

### 必需的 Secrets

1. **KEYSTORE_BASE64**
   - 将你的签名密钥文件（.jks 或 .keystore）转换为 Base64 编码
   - 转换命令：
     ```bash
     base64 -i your-keystore.jks | pbcopy  # macOS
     base64 -w 0 your-keystore.jks > keystore.txt  # Linux
     certutil -encode your-keystore.jks output.txt  # Windows
     ```
   - 将转换后的 Base64 字符串复制到 KEYSTORE_BASE64

2. **KEY_ALIAS**
   - 你的密钥别名
   - 创建密钥时指定的 `-alias` 参数

3. **KEY_PASSWORD**
   - 密钥密码
   - 创建密钥时输入的密码

4. **STORE_PASSWORD**
   - 密钥库密码
   - 创建密钥时输入的 store password

## 本地开发配置

如果你想在本地使用签名配置，可以：

1. 复制 `keystore.properties.example` 为 `keystore.properties`
2. 填入你的实际密钥信息
3. 将 `keystore.properties` 添加到 `.gitignore`（已默认添加）

## 创建签名密钥

如果你还没有签名密钥，可以使用以下命令创建：

```bash
keytool -genkey -v -keystore your-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias your-key-alias
```

## Workflow 触发方式

- **自动触发**：推送到 `main` 或 `master` 分支
- **手动触发**：在 GitHub Actions 页面点击 "Run workflow"
- **PR 触发**：创建 Pull Request 时触发（仅构建 debug 版本）

## 下载构建产物

1. 进入 GitHub Actions 页面
2. 选择最近的 workflow run
3. 在 Artifacts 部分下载：
   - `debug-apk`：调试版本（保留30天）
   - `release-apk`：发布版本（保留90天，已签名）
