# buildscan 工具

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://cleansource.cn) [![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]() [![Java](https://img.shields.io/badge/java-8+-orange.svg)]() [![Platform](https://img.shields.io/badge/platform-linux%20%7C%20macOS%20%7C%20windows-lightgrey)]() ![Crates.io License](https://img.shields.io/badge/License-GPL%20v3-%23FF4136.svg)

🔍 buildscan 是 CleanSource SCA 社区版的 CLI 工具，包含包管理器依赖识别、源码指纹生成两个功能模块，独立于扫描引擎可在本地完成依赖识别与指纹生成，并将识别结果发送给扫描引擎，引擎会通过指纹文件进行源码层面的成分分析，同时识别开源组件的合规与安全风险。

## 🚀 使用介绍

本项目将开放核心 CLI 工具的源代码，您可以基于源码进行二次开发与扩展，以满足自定义需求或集成到企业内部系统中。

### 🧰 本地编译打包

项目使用 **Maven** 作为构建工具，运行需要 **JDK 8+**。

```bash
git clone https://github.com/your-org/cleansource-sca-cli.git
cd cleansource-sca-cli
mvn clean package -DskipTests
```

构建成功后生成的可执行文件在：

```
target/xxx.jar
```

**执行扫描命令**

```
java -jar build_scan.jar \
  --taskDir=/path/to/your/code \
  --username=your@email.com \
  --password=your_password \
  --logLevel=INFO \
  --outputPath=/path/to/temp/output
```

| 参数           | 说明                           | 是否必填 | 备注               |
| -------------- | ------------------------------ | -------- | ------------------ |
| `--username`   | Web 平台用户名（邮箱）         | ✅       |                    |
| `--password`   | 登录密码                       | ✅       |                    |
| `--taskDir`    | 待扫描目录（绝对路径）         | ✅       |                    |
| `--outputPath` | 日志与临时文件输出路径         | ❌       | 默认在系统用户目录 |
| `--logLevel`   | 日志等级：INFO / DEBUG / TRACE | ❌       | 默认 INFO          |

---

## 🧱 技术架构

### 📦 功能结构

| 功能     | 功能描述                           |
| -------- | ---------------------------------- |
| 指纹生成 | 生成文件 hash 与代码片段信息       |
| 包管理器 | 动态识别并调用语言检测器           |
| 任务创建 | 与 Web 平台对接上传任务            |
| 日志级别 | 支持输出详细日志与错误排查辅助信息 |

---

### 🌐 支持的包管理器

| 包管理器  | 语言   | 构建支持       | 检测说明                                         |
| --------- | ------ | -------------- | ------------------------------------------------ |
| Maven     | Java   | ✅             | 使用 `mvn dependency:tree`，或直接解析 `pom.xml` |
| Pip       | Python | ✅             | 解析 `setup.py`、`requirements.txt`              |
| Pipenv    | Python | ✅             | 解析 `Pipfile`、`Pipfile.lock`                   |
| Go mod    | Go     | ✅             | 检查 `go.mod`，依赖 `go` 可执行文件              |
| Go dep    | Go     | ❌（仅非构建） | 解析 `Gopkg.lock`                                |
| Go vendor | Go     | ❌（仅非构建） | 解析 `vendor.json`                               |

---

## 🖥️ 运行环境要求

- **操作系统**：Linux / macOS / Windows
- **JDK**：Java 8 及以上
- **依赖工具**（根据项目语言自动调用）：
  - Maven / Python / pip / pipenv / go 等

---

## 📄 说明

该工具作为清源 SCA 社区版的一部分，仅授权用于企业内部自用或研究用途。如需商业授权或深度定制，请联系官方团队。

### ✅ 欢迎使用清源 SCA 社区版服务

登录 [清源 SCA 社区版](https://cleansource-ce.sectrend.com.cn:9988/) 平台，点击右侧下载图标，获取 `buildscan.jar`。

具体可参考：[帮助文档](https://cleansource-ce.sectrend.com.cn:9988/document/zh/cli-guide/introduction.html)

---

## 📬 问题与建议

- 技术支持

  - 提交 issue

  - 微信: Sectrend

- 官网：[sectrend.com.cn](https://www.sectrend.com.cn/CleanSourceSCA)

- 邮箱：<Info@sectrend.com.cn>
