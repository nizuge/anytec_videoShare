package cn.anytec.util;

import java.io.*;
import java.util.Arrays;

public class Utils {

    /**
     * 输入流转字节数组
     * @param inputStream
     * @return
     */
    public static byte[] streamToByte(InputStream inputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c = 0;
        byte[] buffer = new byte[8 * 1024];
        try {
            while ((c = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, c);
                baos.flush();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     * 级联删除文件夹下所有及本身
     * @param dir
     * @return
     */
    public static boolean clearDir(File dir){
        if(!dir.exists()){
            return true;
        }
        if (dir.isDirectory()) {
            String[] childrens = dir.list();
            if(childrens.length == 0){
                dir.delete();
                return true;
            }
            Arrays.stream(dir.listFiles()).forEach((child)->{
                if(child.isDirectory()){
                    clearDir(child);
                }else {
                    child.delete();
                }
            });
        }
        if(dir.delete())
            return true;
        return false;
    }

    /**
     * 字节数组转16进制字符串
     * @param bArray
     * @return
     */
    public static  String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将本地文件写入到其他路径
     * @param source
     * @param output
     */
    public static void writeFileToOtherPath(File source,File output){
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(output);
            byte[] anVideo = Utils.streamToByte(is);
            if(anVideo != null){
                os.write(anVideo);
                os.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
