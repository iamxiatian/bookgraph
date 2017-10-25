网图知识图谱构建与分析平台
======================

## 安装部署

1. 假设编译打包后的文件所在的目录均为于`netbook`目录下。

2. 安装启动Redis

程序默认访问本机127.0.0.1的默认Redis端口，如果变更，请修改conf/my.conf文件中的
对应参数。

3. 安装JDK1.8最新版，并配置JAVA_HOME,保证java命令出现在PATH环境变量中

4. 运行

在Linux系统下，进入nlp目录，执行：

```bash
    nohup bin/netbook --server &
```

如果是windows系统，打开命令行，进入舆情画像发布文件所在目录，执行：
```bash
    bin/netbook --server
```

此时，即启动了nlp服务，可以直接使用其中的舆情画像服务了。
