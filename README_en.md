# buildscan

English | [中文](./README.md)

[![Version](https://img.shields.io/badge/version-4.1.0-blue.svg)](https://cleansource.cn) [![Java](https://img.shields.io/badge/java-8+-orange.svg)]() [![Platform](https://img.shields.io/badge/platform-linux%20%7C%20macOS%20%7C%20windows-lightgrey)]() ![Crates.io License](https://img.shields.io/badge/License-GPL%20v3-%23FF4136.svg)

🔍 BuildScan is the CLI tool for the CleanSource SCA Community Edition, featuring two functional modules: package manager dependency identification and source code fingerprint generation. Operating independently of the scanning engine, it can locally complete dependency identification and fingerprint generation, then send the results to the scanning engine. The engine will perform component analysis at the source code level using the fingerprint files, while also identifying compliance and security risks in open-source components.

## 🚀 Instructions

This project open-source the core CLI tool, allowing you to perform customization based on the code to meet specific requirements or integrate it into internal enterprise systems.

### 🧰 Local Build and Packaging

The project uses **Maven** as the build tool and requires **JDK 8+** to run.

```bash
git clone https://github.com/sectrend-cn/build-scan.git
cd build-scan
mvn clean package -DskipTests
```

The location of executable file generated after a successful build:

```
target/xxx.jar
```

**Run Scan Command**

```
java -jar build_scan.jar \
  --taskDir=/path/to/your/code \
  --username=your@email.com \
  --password=your_password \
  --logLevel=INFO \
  --outputPath=/path/to/temp/output
```

| Parameter      | Instruction                      | Required | Notes                                  |
| -------------- | -------------------------------- | -------- | -------------------------------------- |
| `--username`   | Web Username（Email）            | ✅       |                                        |
| `--password`   | Password                         | ✅       |                                        |
| `--taskDir`    | Scan target path（Absolute）     | ✅       |                                        |
| `--outputPath` | Path for log and temporary files | ❌       | Default is the system's user directory |
| `--logLevel`   | Log level：INFO / DEBUG / TRACE  | ❌       | Default:INFO                           |

## 📦 Features & Description

| Feature                | Description                                            |
| ---------------------- | ------------------------------------------------------ |
| Fingerprint Generation | Generates file hashes and code fragment information    |
| Package Manager        | Dynamically identifies and invokes language detectors  |
| Task Creation          | Interfaces with web platform for task uploads          |
| Log Level              | Supports detailed logs and troubleshooting diagnostics |

---

### 🌐 Supported Package Managers

| Package Manager | Language | Build Supported      |
| --------------- | -------- | -------------------- |
| Maven           | Java     | ✅                   |
| Pip             | Python   | ✅                   |
| Pipenv          | Python   | ✅                   |
| Go mod          | Go       | ✅                   |
| Go dep          | Go       | ❌（only non-build） |
| Go vendor       | Go       | ❌（only non-build） |

---

## 🖥️ System Requirements

- **OS**：Linux / macOS / Windows
- **JDK**：Java 8+
- **Dependency**（Automatically invokes based on project language）：
  - Maven / Python / pip / pipenv / go, etc

---

## 📄 Notes

This tool, as part of the CleanSource SCA Community Edition, is licensed solely for internal enterprise use or research purposes. For commercial licensing or deep customization, please contact our official team.

### ✅ Welcome to CleanSource SCA Community Edition

Login [CleanSource SCA Community Edition](https://cleansource-ce.sectrend.com.cn:9988/) , Click the download icon on the right，download `buildscan.jar`。

Reference：[Documentation](https://cleansource-ce.sectrend.com.cn:9988/document/zh/cli-guide/introduction.html)

---

## 📬 Questions & Comments

- Support

  - Submit issue on Github

  - Wechat: Sectrend

- Email：<info@sectrend.com.cn>
