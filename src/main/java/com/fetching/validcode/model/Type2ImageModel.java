package com.fetching.validcode.model;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by pijing on 17-4-29.
 * 处理类型为2的验证码图片模型类
 */
public class Type2ImageModel implements Serializable{
    private String filePath = null;
    private BufferedImage bimg = null;
    private int width = 0;
    private int height = 0;
    //根据经验取得边框大小
    private int borderWidth = 170;
    private int borderHeight = 48;
    //灰度值的数组
    private double[][] grayValue = new double[5][];
    //切分字符的图像大小
    private int formalDivideWidth = 32;
    private int formalDivideHeight = 32;

    public Type2ImageModel(String filePath){
        this.filePath = filePath;
        try{
            bimg = ImageIO.read(new File(filePath));
            this.width = bimg.getWidth();
            this.height = bimg.getHeight();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public BufferedImage getBimg(){
        return this.bimg;
    }

    /**
     * @param rgb
     * @return false非白色,true白色
     */
    public boolean isWhite(int rgb){
        int r = (rgb & 16711680) >> 16;
        int g = (rgb & 65280) >> 8;
        int b = (rgb & 255);
        if(r >= 200 && g>=200  && b>= 200){
            return true;
        }
        return false;
    }

    /**
     *
     * @return 获取图片的像素边缘
     */
    public int[] getBorder(){
        int[] border = new int[4];
        try{
            int leftX = -1;
            int rightX = -1;
            int topY = -1;
            int bottomY = -1;

            //去除噪音数据，噪音是黑色块，即上下左右均是黑色的块
            int tempWidth = bimg.getWidth();
            int tempHeight = bimg.getHeight();
            //记录m,n
            ArrayList<Integer[]> recordList = new ArrayList<Integer[]>();
            for(int m=1;m<tempWidth-1;m++){
                for(int n=1;n<tempHeight-1;n++){
                    if(!isWhite(bimg.getRGB(m - 1, n)) && !isWhite(bimg.getRGB(m + 1, n)) &&
                            !isWhite(bimg.getRGB(m, n - 1)) && !isWhite(bimg.getRGB(m, n + 1))){
                        recordList.add(new Integer[]{m,n});
                    }
                }
            }
            //将噪音区域置为白色
            for(int m=0;m<recordList.size();m++){
                bimg.setRGB(recordList.get(m)[0], recordList.get(m)[1], Color.WHITE.getRGB());
            }

            //获取最左侧的非白色像素位置
            for(int i=0;i<width&&leftX==-1;i++){
                for(int j=0;j<height;j++){
                    int rgb = bimg.getRGB(i,j);
                    if(!isWhite(rgb)){
                        leftX = i;
                        break;
                    }
                }
            }
            //获取最右侧的非白色像素位置
            for(int i=width-1;i>=0&&rightX==-1;i--){
                for(int j=0;j<height;j++){
                    int rgb = bimg.getRGB(i,j);
                    if(!isWhite(rgb)){
                        rightX = i;
                        break;
                    }
                }
            }
            //获取最上的非白色像素位置
            for(int i=0;i<height&&topY==-1;i++){
                for(int j=0;j<width;j++){
                    int rgb = bimg.getRGB(j,i);
                    if(!isWhite(rgb)){
                        topY = i;
                        break;
                    }
                }
            }
            //获取最下方的非白色像素的位置
            for(int i=height-1;i>=0&&bottomY==-1;i--){
                for(int j=0;j<width;j++){
                    int rgb = bimg.getRGB(j,i);
                    if(!isWhite(rgb)){
                        bottomY = i;
                        break;
                    }
                }
            }

            int x = leftX;
            int y = topY;
            int width = rightX - leftX + 1;
            int height = bottomY - topY + 1;
            border = new int[]{x,y,width,height};
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return border;
    }

    /**
     *
     * @return 返回按边框截取的图片,并按照统一的长宽(170,480)进行缩放
     */
    public BufferedImage cutImage(){
        int[] border = getBorder();
        Image scaleImage = bimg.getSubimage(border[0],border[1],border[2],border[3]).
                getScaledInstance(borderWidth,borderHeight,Image.SCALE_SMOOTH);
        BufferedImage newImg = new BufferedImage(borderWidth,borderHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) newImg.getGraphics();
        g.drawImage(scaleImage,0,0,borderWidth,borderHeight,null);
        g.dispose();
        return newImg;
    }

    /**
     * @return 由于待识别的验证码的图片的数字就为5个,所以可以按照5对于图片进行切割
     */
    public BufferedImage[] divideImage(){
        BufferedImage curImg = cutImage();
        BufferedImage[] imgArr = new BufferedImage[5];
        try{
            int divideWidth = borderWidth/5;
            int divideHeight = borderHeight;
            Color myWhite = new Color(255, 255, 255); // Color white
            Color myBlack = new Color(0, 0, 0); // Color black

            //图片二值化
            for(int i=0;i<curImg.getWidth();i++){
                for(int j=0;j<curImg.getHeight();j++){
                    if(!isWhite(curImg.getRGB(i,j))){
                        curImg.setRGB(i,j,myBlack.getRGB());
                    }else{
                        curImg.setRGB(i,j,myWhite.getRGB());
                    }
                }
            }

            for(int i=0;i<5;i++){
                int x = divideWidth*i;
                if(i == 0){
                    imgArr[i] = curImg.getSubimage(x,0,divideWidth+4,divideHeight);
                    //在左边以4*divideHeight填充
                    BufferedImage tempImg = new BufferedImage(divideWidth+8,divideHeight,BufferedImage.TYPE_INT_RGB);
                    for(int m=0;m<4;m++){
                        for(int n=0;n<divideHeight;n++){
                            tempImg.setRGB(m,n,myWhite.getRGB());
                        }
                    }
                    Graphics2D g2 = (Graphics2D)tempImg.getGraphics();
                    g2.drawImage(imgArr[i],4,0,null);
                    g2.dispose();
                    imgArr[i] = tempImg;
                }else if(i == 4){
                    x = x - 4;
                    imgArr[i] = curImg.getSubimage(x,0,divideWidth+4,divideHeight);
                    //在右边以4*divideHeight填充
                    BufferedImage tempImg = new BufferedImage(divideWidth+8,divideHeight,BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = (Graphics2D)tempImg.getGraphics();
                    g2.drawImage(imgArr[i],0,0,null);
                    g2.dispose();
                    for(int m=divideWidth+4;m<divideWidth+8;m++){
                        for(int n=0;n<divideHeight;n++){
                            tempImg.setRGB(m,n,myWhite.getRGB());
                        }
                    }
                    imgArr[i] = tempImg;
                }else{
                    x = x-4;
                    imgArr[i] = curImg.getSubimage(x,0,divideWidth+8,divideHeight);
                }

                /*
                //去除噪音数据，噪音是黑色块，即上下左右均是黑色的块
                int tempWidth = imgArr[i].getWidth();
                int tempHeight = imgArr[i].getHeight();
                //记录m,n
                ArrayList<Integer[]> recordList = new ArrayList<Integer[]>();
                for(int m=1;m<tempWidth-1;m++){
                    for(int n=1;n<tempHeight-1;n++){
                        if(!isWhite(imgArr[i].getRGB(m-1,n)) && !isWhite(imgArr[i].getRGB(m+1,n)) &&
                                !isWhite(imgArr[i].getRGB(m,n-1)) && !isWhite(imgArr[i].getRGB(m,n+1))){
                            recordList.add(new Integer[]{m,n});
                        }
                    }
                }
                //将噪音区域置为白色
                for(int m=0;m<recordList.size();m++){
                    imgArr[i].setRGB(recordList.get(m)[0],recordList.get(m)[1],myWhite.getRGB());
                }*/
                //统一缩放为32*32大小
                Image tempTransImg = imgArr[i].getScaledInstance(formalDivideWidth,formalDivideHeight,Image.SCALE_SMOOTH);

                BufferedImage tempImg = new BufferedImage(formalDivideWidth,formalDivideHeight,BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = (Graphics2D)tempImg.getGraphics();
                g2.drawImage(tempTransImg,0,0,formalDivideWidth,formalDivideHeight,null);
                g2.dispose();

                imgArr[i] = tempImg;
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return imgArr;
    }

    /**
     * @param rgb
     * @return 根据rgb计算灰度值
     */
    public double getGrayValue(int rgb){
        int r = (rgb & 16711680) >> 16;
        int g = (rgb & 65280) >> 8;
        int b = (rgb & 255);
        return ( r*38 +  g * 75 +  b * 15 )>>7;
    }

    /**
     * 得到5张图片的灰度值数组
     */
    public void calGrayArray(){
        BufferedImage[] arr = divideImage();
        for(int i=0;i<arr.length;i++){
            grayValue[i] = new double[arr[i].getWidth()*arr[i].getHeight()];
            for(int m=0;m<arr[i].getHeight();m++){
                for(int n=0;n<arr[i].getWidth();n++){
                    grayValue[i][m*arr[i].getWidth()+n] = getGrayValue(arr[i].getRGB(n,m));
                }
            }
        }
    }

    public double[][] getGrayValue(){
        return this.grayValue;
    }

    public static void main(String[] args){
        String[] filePaths = new String[]{
                "F:/validcode_solution/type2_test2_1.jpg",
                "F:/validcode_solution/type2_test2_2.jpg"//,
//                "F:/validcode_solution/type2_test2_3.jpg",
//                "F:/validcode_solution/type2_test2_4.jpg",
//                "F:/validcode_solution/type2_test2_5.jpg",
//                "F:/validcode_solution/type2_test2_6.jpg",
//                "F:/validcode_solution/type2_test2_7.jpg",
//                "F:/validcode_solution/type2_test2_8.jpg",
//                "F:/validcode_solution/type2_test2_9.jpg",
//                "F:/validcode_solution/type2_test2_10.jpg"
        };
        int index = -1;
        for(String filePath:filePaths){
            index++;
            Type2ImageModel type2ImageModel = new Type2ImageModel(filePath);
             /*
            int[] border = type2ImageModel.getBorder();
            for(int i=0;i<border.length;i++){
                System.out.print(border[i]+" ");
            }
            System.out.println();
            //绘制边框以供验证
            Graphics2D g2 = (Graphics2D) type2ImageModel.getBimg().createGraphics();
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawRect(border[0], border[1], border[2], border[3]);
            g2.dispose();
            try {
                ImageIO.write(type2ImageModel.getBimg(),"jpg",
                        new File("F:/validcode_solution/reco_"+index+".jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedImage cutImage = type2ImageModel.cutImage();
            try {
                ImageIO.write(cutImage,"jpg",
                        new File("F:/validcode_solution/cut_"+index+".jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            BufferedImage[] tempArr = type2ImageModel.divideImage();
            for(int m=0;m<tempArr.length;m++){
                try {
                    ImageIO.write(tempArr[m],"jpg",
                            new File("F:/validcode_solution/divide_"+index+"_"+m+".jpg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            type2ImageModel.calGrayArray();
            double[][] grayArr = type2ImageModel.getGrayValue();
            for(int i=0;i<grayArr.length;i++){
                for(int j=0;j<grayArr[i].length;j++){
                    System.out.print(grayArr[i][j]+" ");
                }
                System.out.println();
            }
            System.out.println();
        }
        System.out.println("done!");
    }
}
