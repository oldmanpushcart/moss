# MOSS 桌面个人助手

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK8+](https://img.shields.io/badge/JDK-17+-blue.svg)
![JavaFx17+](https://img.shields.io/badge/JavaFx-17+-blue.svg)
![LLM-通义千问](https://img.shields.io/badge/LLM-%E9%80%9A%E4%B9%89%E5%8D%83%E9%97%AE-blue.svg)

![LOGO](https://github.com/oldmanpushcart/moss/wiki/images/moss-splash-logo.png)

> MOSS 是基于通义千问大模型的桌面助手，采用JavaFx开发，纯个人娱乐使用。

## 一、功能特性

### 当前支持

#### 内置功能

1. **多模态生成**：文本生成、文生图、文生视频、图生图、图生视频
2. **多模态理解**：文档、图片、音频、视频理解
3. **语音播报**：可以对生成内容进行语音播报
4. **检索增强（RAG）**：可以对文档进行索引进入内置知识库

检索增强（RAG）使用 [sqlite-vec](https://alexgarcia.xyz/sqlite-vec/) 实现，纯C实现的SQLITE扩展，需要针对操作系统、CPU架构进行编译。
当前我已经编译好了以下支持，你可以在以下环境下预编译完毕，你可以直接启动程序。如果你需要支持其他的环境，请联系我。

|操作系统|CPU架构|
|---|---|
|MACOS|ARRCH64|
|WINDOWS|X64|

#### 外部扩展支持

1. **高德地图**：IP定位、地图展示、地址查询、出行线路规划
2. **CALDAV**：日历事件的查询、创建、删除

### 界面展示

![软件屏幕截图](https://github.com/oldmanpushcart/moss/wiki/images/moss-scrollshort-001.png)

### 未来计划

- 计划支持 SMTP
- 计划支持 MCP Server

## 二、快速使用

1. **签出仓库**

   ```shell
   git clone https://github.com/oldmanpushcart/moss.git
   ```

2. **修改配置**

    - 申请通义千问 API-KEY：[点击申请](https://bailian.console.aliyun.com/apiKey=1?apiKey=1#/api-key)
    - 申请高德地图 API-KEY：[点击申请](https://console.amap.com/dev/key/app)
    - 修改 `/cfg/moss.yml` 文件，将 `api-key` 填写对应的值

3. **运行程序**

   ```shell
   mvn javafx:run
   ```

## 三、工程说明

| 文件 / 目录 | 说明        |
|---------|-----------|
| /cfg    | 配置文件存放目录  |
| /data   | 运行时数据存放目录 |
| /src    | 源代码目录     |
| /logs   | 日志文件存放目录  |

## 四、运行要求

1. JDK17+
2. JavaFx17+