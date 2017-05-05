package com.fetching.validcode.main;
import com.fetching.validcode.common.Constant;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.feature.Normalizer;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.dmg.pmml.DataType;
import java.io.Serializable;
import java.util.*;
/**
 * Created by pijing on 17-5-1.
 */
public class NeuralNetworkPredict implements Serializable {
    private final String filePath = "/validcode2/testdata";
    private final String labelPath = "/home/bsauser";

    public void startApp(){
        DataGenerator dataGenerator = new DataGenerator();
        final Map<String,Integer> labelMap = dataGenerator.readLabel(labelPath);
        //transMap
        final Map<Integer,String> transMap = new HashMap<Integer, String>();
        Iterator<Map.Entry<String,Integer>> iterator = labelMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Integer> entry = iterator.next();
            transMap.put(entry.getValue(),entry.getKey());
        }

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster(Constant.master);
        sparkConf.setAppName(Constant.TRAINAPPNAME);
        SparkContext sparkContext = new SparkContext(sparkConf);
        SQLContext sqlContext = new SQLContext(sparkContext);

        StructType schema = new StructType(new StructField[]{
                new StructField("imgname", DataTypes.StringType, false, Metadata.empty()),
                new StructField("index", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("orgfeatures", new VectorUDT(), false, Metadata.empty())
        });
        StructType schema1 = new StructType(new StructField[]{
                new StructField("imgname", DataTypes.StringType, false, Metadata.empty()),
                new StructField("index", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("label", DataTypes.StringType, false, Metadata.empty())
        });
        JavaRDD<Row> testData = sparkContext.textFile(filePath,4).toJavaRDD().
                map(new Function<String, Row>() {
                    public Row call(String s) throws Exception {
                        String[] arr = s.split(" ");
                        List<Double> rtnList = new ArrayList<Double>();
                        for(int i=2;i<arr.length;i++){
                            if(arr[i].trim().length() != 0){
                                rtnList.add(Double.valueOf(arr[i]));
                            }
                        }
                        double[] doubleArr = new double[rtnList.size()];
                        for(int i=0;i<doubleArr.length;i++){
                            doubleArr[i] = rtnList.get(i);
                        }
                        return RowFactory.create(String.valueOf(arr[0]),Integer.valueOf(String.valueOf(arr[1])),Vectors.dense(doubleArr));
                    }
                });
        Dataset<Row> testDataDs = sqlContext.createDataFrame(testData,schema);

        //归一化的类
        Normalizer normalizer = new Normalizer()
                .setInputCol("orgfeatures")
                .setOutputCol("features")
                .setP(1.0);
        Dataset<Row> testDataNorm = normalizer.transform(testDataDs).select("imgname","index","features");

        MultilayerPerceptronClassificationModel model = MultilayerPerceptronClassificationModel.load("/neuralnetwork_model_20170502_final");
        JavaRDD<Row> result = model.transform(testDataNorm).select("imgname","index","prediction")
                .toJavaRDD().map(new Function<Row, Row>() {
                    public Row call(Row row) throws Exception {
                        return RowFactory.create(String.valueOf(row.get(0)),
                                Integer.valueOf(String.valueOf(row.get(1))),
                                transMap.get(Integer.valueOf(String.format("%.0f",row.getDouble(2)))));
                    }
                });
        Dataset<Row> resultDs = sqlContext.createDataFrame(result,schema1);
        JavaRDD<String> resultReal = resultDs.groupBy("imgname").
                agg(functions.concat_ws(",",functions.collect_set(functions.concat_ws("_",resultDs.col("index"),resultDs.col("label")))).as("curlabel"))
                .select("imgname","curlabel")
                .toJavaRDD().map(new Function<Row, String>() {
                    public String call(Row row) throws Exception {
                        String[] lines = String.valueOf(row.get(1)).split(",");
                        Map<Integer,String> map = new HashMap<Integer,String>();
                        for(String line:lines){
                            map.put(Integer.valueOf(line.split("_")[0]),line.split("_")[1]);
                        }
                        StringBuilder sb = new StringBuilder().append(map.get(0)).append(map.get(1)).append(map.get(2)).
                                append(map.get(3)).append(map.get(4));
                        return row.get(0)+","+sb.toString();
                    }
                });
        //System.out.println(resultReal.take(1));
        resultReal.repartition(1).saveAsTextFile("/validcode2/result_final");
    }

    public static void main(String[] args){
        new NeuralNetworkPredict().startApp();
    }
}
