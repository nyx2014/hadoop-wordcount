# hadoop-wordcount
An hello world project for hadoop, Countinig words in given input.

## compile and package
at the root of hadoop:
$ bin/hadoop com.sun.tools.javac.Main WordCount.java
$ jar cf wc.jar WordCount*.class

## run
$ bin/hadoop jar wc.jar WordCount input output

## show result
$ bin/hadoop fs -cat output/part-r-00000