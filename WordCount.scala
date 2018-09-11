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