import org.apache.spark.{SparkConf, SparkContext}

object SparkML {
  def main(args: Array[String]): Unit = {
    //准备
    val conf = new SparkConf().setAppName("SparkML Demo")
    val sc = new SparkContext(conf)
    val lines = sc.textFile("/path/to/imdb.txt")
    lines.persist()
    lines.count()
    val columns = lines.map(_.split("\\t"))
    columns.first()

    import sqlContext.implicits._
    case class Review(text:String, label: Double)
//    SparkSession.builder()
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    val reviews = columns.map(a=> Review(a(0),a(1).toDouble)).toDF()
    //完整性检查
    reviews.printSchema()
    reviews.groupBy("label").count().show()

    //分割测试集
    val Array(trainingData, testData) = reviews.randomSplit(Array(0.8,0.2))
    trainingData.count()
    testData.count()

    //创建特征向量
    import org.apache.spark.ml.feature.Tokenizer
    val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    //检查输出的DataFrame的格式
    val tokenizedData = tokenizer.transform(trainingData)

    //创建表示评价的特征向量
    import org.apache.spark.ml.feature.HashingTF
    val hashingTF = new HashingTF()
      .setNumFeatures(1000)
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    //检查输出的DataFrame的格式
    val hashedData = hashingTF.transform(tokenizedData)

    //创建Estimator来训练模型
    import org.apache.spark.ml.classification.LogisticRegression
    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.01)

    import org.apache.spark.ml.Pipeline
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer,hashingTF,lr))
    //训练！
    val pipelineModel = pipeline.fit(trainingData)
    //获得在训练集和测试集上的预测
    val testPredictions = pipelineModel.transform(testData)
    val trainingPredictions = pipelineModel.transform(trainingData)

    //二元分类器的评价器对模型评估
    import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
    val evaluator = new BinaryClassificationEvaluator()

    import org.apache.spark.ml.param.ParamMap
    val evaluatorParamMap = ParamMap(evaluator.metricName -> "areaUnderROC")
    //auc值表示模型质量
    val aucTraining = evaluator.evaluate(trainingPredictions,evaluatorParamMap)
    val aucTest = evaluator.evaluate(testPredictions,evaluatorParamMap)

    //超参数调教
    //创建参数集合
    import org.apache.spark.ml.tuning.ParamGridBuilder
    val paramGrid = new ParamGridBuilder()
      .addGrid(hashingTF.numFeatures,Array(10000,100000))
      .addGrid(lr.regParam,Array(0.01,0.1,1.0))
      .addGrid(lr.maxIter,Array(20,30))
      .build()

    import org.apache.spark.ml.tuning.CrossValidator
    val crossValidator = new CrossValidator()
      .setEstimator(pipeline)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(10)
      .setEvaluator(evaluator)
    val crossValidatorModel = crossValidator.fit(trainingData)
    //在测试集上评估
    val newPredictions = crossValidatorModel.transform(testData)
    val newAucTest = evaluator.evaluate(newPredictions,evaluatorParamMap)
    //最佳模型
    val bestModel = crossValidatorModel.bestModel
  }
}
