package cn.anytec.quadrant.hcService;

import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.hcsdk.HCNetSDK;
import cn.anytec.quadrant.hcEntity.DeviceInfo;
import com.sun.jna.NativeLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.*;

@Component
public class HCSDKHandler {

    private static final Logger logger = LoggerFactory.getLogger(HCSDKHandler.class);

    public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    public Map<String, DeviceInfo> deviceInfoMap = new HashMap<>();

    @Autowired
    GeneralConfig config;
    @Autowired
    SpringBootListener listener;

    @PostConstruct
    private void init() {
        if (hCNetSDK.NET_DVR_Init()) {
            logger.info("=====初始化HCNETSDK成功=====");
        } else {
            logger.error("Error：初始化HCNETSDK失败");
        }
        hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
        hCNetSDK.NET_DVR_SetReconnect(10000, true);
        hCNetSDK.NET_DVR_SetLogToFile(true, null, false);
        MyExceptionCallBack myExceptionCallBack = new MyExceptionCallBack();
        hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, 0, myExceptionCallBack, null);
    }

    public boolean loginCamera(DeviceInfo deviceInfo) {
        if (deviceInfoMap.containsKey(deviceInfo.getDeviceIp())) {
            logger.warn(deviceInfo.getDeviceIp() + " 该设备IP已注册");
            return true;
        }
        HCNetSDK.NET_DVR_USER_LOGIN_INFO struLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();
        HCNetSDK.NET_DVR_DEVICEINFO_V40 struDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();
        for (int i = 0; i < deviceInfo.getDeviceIp().length(); i++) {
            struLoginInfo.sDeviceAddress[i] = (byte) deviceInfo.getDeviceIp().charAt(i);
        }
        for (int i = 0; i < deviceInfo.getUserName().length(); i++) {
            struLoginInfo.sUserName[i] = (byte) deviceInfo.getUserName().charAt(i);
        }
        for (int i = 0; i < deviceInfo.getPassword().length(); i++) {
            struLoginInfo.sPassword[i] = (byte) deviceInfo.getPassword().charAt(i);
        }
        struLoginInfo.wPort = deviceInfo.getPort();
        struLoginInfo.bUseAsynLogin = 0;
        struLoginInfo.write();
        NativeLong userId = hCNetSDK.NET_DVR_Login_V40(struLoginInfo.getPointer(), struDeviceInfo.getPointer());
        if (userId.longValue() >= 0 && hCNetSDK.NET_DVR_GetLastError() == 0) {
            logger.info(deviceInfo.getDeviceIp() + " 注册设备成功");
            deviceInfo.setDeviceId(userId);
            deviceInfoMap.put(deviceInfo.getDeviceIp(), deviceInfo);
            return true;
        } else {
            logger.error(deviceInfo.getDeviceIp() + " 注册失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        }
    }

    public boolean loginout(DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId() == null) {
            logger.warn(deviceInfo.getDeviceIp() + " 该设备尚未注册");
            return false;
        }
        if (!hCNetSDK.NET_DVR_Logout(deviceInfo.getDeviceId())) {
            logger.error(deviceInfo.getDeviceIp() + " 注销设备失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        }
        if (deviceInfoMap.remove(deviceInfo.getDeviceIp(), deviceInfo)) {
            logger.info(deviceInfo.getDeviceIp() + " 注销设备成功");
            return true;
        }
        return false;
    }

    public NativeLong preView(DeviceInfo deviceInfo,HCNetSDK.FRealDataCallBack_V30 callBack_v30) {
        NativeLong lRealPlayHandle = null;
        HCNetSDK.NET_DVR_PREVIEWINFO strPreviewInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
        strPreviewInfo.lChannel = new NativeLong(1);//预览通道号
        strPreviewInfo.hPlayWnd = null;//需要SDK解码时句柄设为有效值，仅取流不解码时可设为空
        strPreviewInfo.dwStreamType = 1;//0-主码流，1-子码流，2-码流3，3-码流4，以此类推
        strPreviewInfo.dwLinkMode = 0;//0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4-RTP/RTSP，5-RSTP/HTTP
        logger.info(deviceInfo.getDeviceId()+"ID");
        lRealPlayHandle = hCNetSDK.NET_DVR_RealPlay_V40(deviceInfo.getDeviceId(), strPreviewInfo, callBack_v30, null);
        if (!(lRealPlayHandle.longValue() >= 0 && hCNetSDK.NET_DVR_GetLastError() == 0)) {
            logger.error("预览失败,错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return null;
        }
        return lRealPlayHandle;
    }



    public void stopPreView(NativeLong lReadPlayHandle){
        if(!hCNetSDK.NET_DVR_StopRealPlay(lReadPlayHandle)){
            logger.error("stop preview failed");
        }
    }

    @PreDestroy
    public void cleanUp() {
        hCNetSDK.NET_DVR_Cleanup();
    }
}
