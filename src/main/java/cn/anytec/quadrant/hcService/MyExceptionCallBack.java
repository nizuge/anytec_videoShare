package cn.anytec.quadrant.hcService;

import cn.anytec.hcsdk.HCNetSDK;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyExceptionCallBack implements HCNetSDK.FExceptionCallBack {

    private static final Logger logger = LoggerFactory.getLogger(MyExceptionCallBack.class);

    public void invoke(int dwType, NativeLong lUserID, NativeLong lHandle, Pointer pUser) {
        logger.error("异常回调函数");
        logger.error("异常类型："+String.format("%04x", dwType));
        logger.error("异常ID："+lUserID.longValue());
        logger.error("异常句柄："+lHandle);
    }
}
