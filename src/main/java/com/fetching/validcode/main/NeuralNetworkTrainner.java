package com.fetching.validcode.main;
import com.fetching.validcode.common.Constant;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.Normalizer;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
/**
 * Created by pijing on 17-4-30.
 */
public class NeuralNetworkTrainner implements Serializable{
    private final String filePath = "/validcode2/traindata";
    private final String labelPath = "/home/bsauser";

    public void startApp(){
        DataGenerator dataGenerator = new DataGenerator();
        final Map<String,Integer> labelMap = dataGenerator.readLabel(labelPath);

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster(Constant.master);
        sparkConf.setAppName(Constant.TRAINAPPNAME);
        SparkContext sparkContext = new SparkContext(sparkConf);
        SQLContext sqlContext = new SQLContext(sparkContext);

        StructType schema = new StructType(new StructField[]{
                new StructField("label", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("orgfeatures", new VectorUDT(), false, Metadata.empty())
        });
        StructType schema1 = new StructType(new StructField[]{
                new StructField("label", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("features", new VectorUDT(), false, Metadata.empty())
        });
        JavaRDD<Row> trainData = sparkContext.textFile(filePath,4).toJavaRDD().
                map(new Function<String, Row>() {
                    public Row call(String s) throws Exception {
                        String[] arr = s.split(" ");
                        String label = arr[0];
                        List<Double> rtnList = new ArrayList<Double>();
                        for(int i=1;i<arr.length;i++){
                            if(arr[i].trim().length() != 0){
                                rtnList.add(Double.valueOf(arr[i]));
                            }
                        }
                        double[] doubleArr = new double[rtnList.size()];
                        for(int i=0;i<doubleArr.length;i++){
                            doubleArr[i] = rtnList.get(i);
                        }
                        return RowFactory.create(labelMap.get(label), Vectors.dense(doubleArr));
                    }
                });
        Dataset<Row> trainDataDs = sqlContext.createDataFrame(trainData,schema);

        //归一化的类
        Normalizer normalizer = new Normalizer()
                .setInputCol("orgfeatures")
                .setOutputCol("features")
                .setP(1.0);
        Dataset<Row> trainDataNorm = normalizer.transform(trainDataDs).select("label","features").filter("label is not null");
        trainDataNorm.cache();

        //开始训练,生成神经网络模型
        int[] layers = new int[] {1024, 100, 36};
        MultilayerPerceptronClassifier trainer = new MultilayerPerceptronClassifier()
                .setLayers(layers)
                .setBlockSize(128)
                .setStepSize(3.0)
                .setSeed(1234L)
                .setMaxIter(3000);
        MultilayerPerceptronClassificationModel model = trainer.fit(trainDataNorm);
        try {
            //打印准确率
            Dataset<Row> result = model.transform(trainDataNorm);
            Dataset<Row> predictionAndLabels = result.select("prediction", "label");
            MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                    .setMetricName("accuracy");
            System.out.println("Test set accuracy = " + evaluator.evaluate(predictionAndLabels));

            model.save("/neuralnetwork_model_20170502_final");
            System.out.println("done!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        trainDataNorm.unpersist();
    }

    public static void main(String[] args){
        new NeuralNetworkTrainner().startApp();
    }
}
