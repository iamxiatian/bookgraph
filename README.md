BookGraph
======================

## 安装部署

1. 假设编译打包后的文件所在的目录均为于`bookgraph`目录下。

2. 安装JDK1.8最新版，并配置JAVA_HOME,保证java命令出现在PATH环境变量中

3. 运行

在Linux系统下，进入bookgraph目录，执行：

```bash
    nohup bin/bookgraph --server &
```

如果是windows系统，打开命令行，进入发布文件所在目录，执行：
```bash
    bin/bookgraph --server
```

此时，即启动了服务

参考资料
=============================

PDF
-----------------------------
### Document Structure
In this chapter, we leave behind the bits and bytes of the PDF file, and consider the logical structure. We consider the trailer dictionary, document catalog, and page tree. We enumerate the required entries in each object. We then look at two common structures in PDF files: text strings and dates.

https://www.safaribooksonline.com/library/view/pdf-explained/9781449321581/ch04.html

