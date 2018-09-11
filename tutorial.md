# 目的：在Ubuntu18.04下使用idea进行spark的开发、调试

### 0：准备工作

需要jdk1.8
```
$ sudo apt-get install openjdk-8-jdk
```
//暂时认为不兼容jdk10，具体情况另行讨论

其他工具都暂不需要

### 1：下载spark

http://spark.apache.org/downloads.html

选择最新编译版2.3.1

prebuilt for hadoop 2.7 or later

得到spark-2.3.1-bin-hadoop2.7.tgz

解压后放至/home/user/spark
即可。不需要任何配置

### 2：下载idea

https://www.jetbrains.com/idea/download/#section=linux

下载安装idea

要求旗舰版

安装过程中选择scala插件（必需）

### 3：开工

在idea中 新建gradle项目

填写信息

其余保持默认

在build.gradle中:
```
group 'com.lsr'
version '1.0-DEMO'

apply plugin: 'scala'
sourceCompatibility = 1.8

repositories {
    mavenCentral()
}
sourceSets {
    main {
        scala {
            srcDirs = ['src/scala']
        }
    }
}
dependencies {
    compile group: 'org.scala-lang', name: 'scala-library', version: '2.11'
    compile group: 'org.scala-lang', name: 'scala-compiler', version: '2.11'
    compile group: 'org.scala-lang', name: 'scalap', version: '2.11'
    compile group: 'org.apache.spark', name: 'spark-core_2.11', version: '2.2.0'
}
```

新建目录：src/scala

新建文件：WordCount.scala

此时idea提示配置ScalaSDK

选择create，版本选择2.11.12

//记住，截至目前spark不兼容Scala2.12

完成后开始编写代码

```
object WordCount {
    def main(args: Array[String]): Unit = {
        import org.apache.spark.{SparkConf, SparkContext}
        new SparkContext(
            new SparkConf()
                .setAppName("WordCount")
                .setMaster("spark://my-pc:7077")
        )
            .textFile(args(0))
            .flatMap {line => line.split(" ")}
            .map(word => (word, 1))
            .reduceByKey(_ + _)
            .coalesce(1)
            .saveAsTextFile(args(1))
    }
}
```

### 4：准备输入数据

/home/user/Work/input/in.txt

-> Hello Spark Hello World

### 5：启动集群

启动master

```
$ /home/user/spark/sbin/start-master.sh
```

此时打开浏览器访问：localhost:8080

可以看到spark控制台，页面中显示了spark master的URL

启动slave

```
$ /home/user/spark/sbin/start-slave.sh spark://my-pc:7077
```

### 6：打包

在idea中打开project structure

添加artifact，选择jar，form module

module一定要选第一个，不要选main也不要选test

下面选择extract to the target JAR（一定）

ok后在右侧可用组件中找到WordCount_main右键加入output目录

完成。

此时，在idea的菜单中选择build->build Artifacts->build

等待编译和打包完成。

### 7：运行

打开终端：

```
$ cd ~
$ ./spark/bin/spark-submit --class "WordCount" /home/user/IdeaProjects/WordCount/out/artifacts/WordCount_jar/WordCount.jar /home/user/Work/input /home/user/Work/out
```

### 8：结果

在~/Work/out目录下可以看到输出

在localhost:8080可以看到运行实例，并且可以查看控制台输出

### 9：常见错误

A JNI error has occurred

通常是jdk版本错误