package cn.anytec.quadrant.slideway;

import cn.anytec.hcsdk.HCNetSDK;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;

public class SlideDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {

    private static final Logger logger = LoggerFactory.getLogger(SlideDataCallBack.class);

    private OutputStream outputStream;
    private File videoTmp;

    public SlideDataCallBack(File tmpFile){
        videoTmp = tmpFile;
        File parent = tmpFile.getParentFile();
        if(!parent.exists()){
            if(parent.mkdir()){
                logger.error("创建游客视频处理文件夹失败");
            }
        }
        String fileName = tmpFile.getName();
        if(fileName.equals("far.tmp")){
            if(parent.list().length != 0){
                Arrays.stream(parent.listFiles()).forEach((child) -> {
                    child.delete();
                });
            }
        }
        try {
            outputStream = new FileOutputStream(videoTmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void invoke(NativeLong lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {
        if(dwDataType == HCNetSDK.NET_DVR_STREAMDATA){
            Pointer pointer = pBuffer.getPointer();
            byte[] data = pointer.getByteArray(0,dwBufSize);
            try {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void close(){
        try {
            if(outputStream != null)
                outputStream.close();
            if(videoTmp.exists() && videoTmp.getName().contains("tmp")){
                if(!videoTmp.renameTo(new File(videoTmp.getAbsolutePath().replace("tmp","temp")))){
                    logger.error("滑梯区视频临时文件写入完毕标识失败");
                }
            }else {
                videoTmp.delete();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
