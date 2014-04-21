# FoxBook(狐狸的小说下载阅读及转换工具) Android版

**名称:** FoxBook

**功能:** 狐狸的小说下载阅读及转换工具(下载小说站小说，制作为umd,txt格式)

**作者:** 爱尔兰之狐(linpinger)

**邮箱:** <mailto:linpinger@gmail.com>

**主页:** <http://linpinger.github.io?s=FoxBook-Android_MD>

**缘起:** 用别人写的工具，总感觉不能随心所欲，于是自己写个下载管理工具，基本能用，基本满意

**原理:** 下载网页，分析网页，文本保存在数据库中，转换为其他需要的格式

**亮点:** 通用小说网站规则能覆盖大部分文字站的目录及正文内容分析，不需要针对每个网站的规则

**源码及下载:**

-   [源码工程](https://github.com/linpinger/foxbook-android)

-   [APK文件](https://github.com/linpinger/foxbook-android/releases/download/TestUse/FoxBook.apk)

**工程中包含的其他文件:**
- 生成UMD格式电子书调用umd-builder JAVA版 <http://code.google.com/p/umd-builder/>
- 阅读页面的羊皮纸背景来自: iReaderV1.5.0.1.apk
- 程序图标来自: <http://findicons.com/icon/93370/emblem_library?id=274277> Icon Pack: Human-O2 Designer: schollidesign


**更新日志:**

- 2014-04-21: 界面细微调整，在书籍信息里添加修改isEnd字段
- 2014-04-18: 添加: Agent头部来获取yahoo正确搜索结果链接 注:原生头部和IE8都会导致追踪链接
- 2014-04-17: 添加: 在搜索界面的菜单中添加快速搜索功能，以后可以加入更多搜索引擎
- 2014-04-14: 小修复: 每章节第一个段落头部添加空白
- 2014-04-13: 菜单添加生成txt功能，路径 /sdcard/fox.txt
- 2014-04-07: 发布Android版，和FoxBook共用同一数据库文件，放在sdcard根目录
- ...: 懒得写了，就这样吧
