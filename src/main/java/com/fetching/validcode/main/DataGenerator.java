package com.fetching.validcode.main;
import com.fetching.validcode.model.Type2ImageModel;
import java.io.*;
import java.util.*;

/**
 * Created by pijing on 17-4-30.
 */
public class DataGenerator implements Serializable{
    /**
     *
     * @param srcPath
     * @return 读取训练集数据的标签
     */
    public Map<String,String[]> readFile(String srcPath){
        Map<String,String[]> map = new HashMap<String, String[]>();
        BufferedReader reader = null;

        try{
            reader = new BufferedReader(new FileReader(new File(srcPath)));
            String line = null;
            while((line = reader.readLine()) != null){
                String[] lineArr = line.split(",");
                String[] codes = new String[5];
                for(int i=0;i<codes.length;i++){
                    codes[i] = lineArr[1].substring(i,i+1);
                }
                map.put(lineArr[0],codes);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }finally{
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    /**
     *@param  srcPath
     * @param dstPath
     * 将img转变为数据特征
     */
    public void transImgToDataFile(String srcPath, String dstPath,boolean withLabel){
        PrintWriter out = null;
        try{
            out = new PrintWriter(new File(dstPath));
            Map<String,String[]> labelData = null;
            if(withLabel){
                labelData = readFile(srcPath+"/type2_train.csv");
            }
            File file = new File(srcPath);
            String[] files = file.list();
            for(String fileItem:files){
                if(fileItem.endsWith(".jpg")){
                    String fullPath = srcPath+"/"+fileItem;
                    Type2ImageModel model = new Type2ImageModel(fullPath);
                    model.calGrayArray();
                    double[][] grayValue = model.getGrayValue();
                    String[] code = null;
                    if(withLabel){
                        code = labelData.get(fileItem);
                    }
                    for(int i=0;i<grayValue.length;i++){
                        StringBuilder sb = new StringBuilder();
                        if(withLabel){
                            sb.append(code[i]+" ");
                        }else{
                            sb.append(fileItem+" "+i+" ");
                        }

                        for(double grayValueItem:grayValue[i]){
                            sb.append(grayValueItem+" ");
                        }
                        String writeLine = sb.toString().substring(0,sb.toString().length()-1);
                        out.println(writeLine);
                    }
                }
            }
            out.flush();
        }catch(Exception ex){
            ex.printStackTrace();
        }finally{
            if(out != null){
                out.close();
            }
        }
    }

    /**
     *
     * @param srcPath
     * @return 读取标签数据
     */
    public Map<String,Integer> readLabel(String srcPath){
        Map<String,String[]> labels = readFile(srcPath+"/type2_train.csv");
        Set<String> set = new HashSet<String>();
        Iterator<Map.Entry<String,String[]>> iterator = labels.entrySet().iterator();
        while(iterator.hasNext()){
            String[] labelArr = iterator.next().getValue();
            for(String labelItem:labelArr){
                set.add(labelItem);
            }
        }
        Map<String,Integer> map = new HashMap<String, Integer>();
        Object[] setArr = set.toArray();
        for(int i=0;i<setArr.length;i++){
            map.put(String.valueOf(setArr[i]),i);
            //System.out.println(String.valueOf(setArr[i]));
        }

        return map;
    }

    public static void main(String[] args){
        DataGenerator generator = new DataGenerator();
        generator.transImgToDataFile("F:/validcode_solution/type2_train",
                "F:/validcode_solution/traindata",true);
        generator.transImgToDataFile("F:/validcode_solution/type2_test2",
                "F:/validcode_solution/testdata",false);
    }
}
