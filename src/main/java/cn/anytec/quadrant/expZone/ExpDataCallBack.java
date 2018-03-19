package cn.anytec.quadrant.expZone;

import cn.anytec.hcsdk.HCNetSDK;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExpDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {

    private static final Logger logger = LoggerFactory.getLogger(ExpDataCallBack.class);

    private FileOutputStream fileOutputStream;
    private File context;
    private File videoTmp;

    public ExpDataCallBack(String path, String fileName){
        context = new File(path);
        if(!context.exists()){
            context.mkdirs();
        }
        videoTmp = new File(context,fileName);
        try {
            fileOutputStream = new FileOutputStream(videoTmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void invoke(NativeLong lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {
        if(dwDataType == HCNetSDK.NET_DVR_STREAMDATA){
            Pointer pointer = pBuffer.getPointer();
            byte[] data = pointer.getByteArray(0,dwBufSize);
            try {
                fileOutputStream.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void close(){
        try {
            if(fileOutputStream != null)
                fileOutputStream.close();
            if(videoTmp.exists() && videoTmp.getName().contains("tmp")){
                if(!videoTmp.renameTo(new File(videoTmp.getAbsolutePath().replace("tmp","temp")))){
                    logger.error("体验区视频临时文件写入完毕标识失败");
                }
            }else {
                videoTmp.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
