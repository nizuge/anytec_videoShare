package cn.anytec.quadrant.expZone;

import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.mongo.MongoDB;
import cn.anytec.quadrant.hcEntity.DeviceInfo;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import com.sun.jna.NativeLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ExpZoneService {
    private static final Logger logger = LoggerFactory.getLogger(HCSDKHandler.class);

    private static List<String> bumperCarVisitorIdList = new ArrayList<>();
    private static List<String> toyCarVisitorIdList = new ArrayList<>();
    private static List<String> arAreaVisitorIdList = new ArrayList<>();
    private List<DeviceInfo> bumperCarDeviceList = new ArrayList<>();
    private Map<String,List<String>> pathVisitorIdMap = new HashMap<>();
    private DeviceInfo toyCarDevice;
    private DeviceInfo arAreaDevice;
    ExecutorService areaTaskPool = Executors.newCachedThreadPool();

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    SpringBootListener listener;
    @Autowired
    MongoDB mongoDB;

    //添加游客Id
    public boolean addVisitorId(String visitorId, String place) {
        if (place.equals(config.getBumperCar())) {
            bumperCarVisitorIdList.add(visitorId);
        } else if (place.equals(config.getToyCar())) {
            toyCarVisitorIdList.add(visitorId);
        } else if (place.equals(config.getArArea())) {
            arAreaVisitorIdList.add(visitorId);
        } else {
            return false;
        }
        return true;
    }

    //移除游客Id
    public boolean removeVisitorId(String visitorId, String place) {
        if (place.equals(config.getBumperCar())) {
            if (bumperCarVisitorIdList.size() > 0) {
                bumperCarVisitorIdList.remove(visitorId);
            }
        } else if (place.equals(config.getToyCar())) {
            if (toyCarVisitorIdList.size() > 0) {
                toyCarVisitorIdList.remove(visitorId);
            }
        } else if (place.equals(config.getArArea())) {
            if (arAreaVisitorIdList.size() > 0) {
                arAreaVisitorIdList.remove(visitorId);
            }
        } else {
            return false;
        }
        return true;
    }

    //清空游客Ids
    public boolean clearVisitorIds(String place) {
        if (place.equals(config.getBumperCar())) {
            bumperCarVisitorIdList.clear();
        } else if (place.equals(config.getToyCar())) {
            toyCarVisitorIdList.clear();
        } else if (place.equals(config.getArArea())) {
            arAreaVisitorIdList.clear();
        } else {
            return false;
        }
        return true;
    }

    //返回游客IdList
    public List<String> getVisitorIdList(String place) {
        if (place.equals(config.getBumperCar())) {
            return bumperCarVisitorIdList;
        } else if (place.equals(config.getToyCar())) {
            return toyCarVisitorIdList;
        } else if (place.equals(config.getArArea())) {
            return arAreaVisitorIdList;
        } else {
            return null;
        }
    }

    public Map<String,List<String>> getPathVisitorIdMap(){
        return pathVisitorIdMap;
    }

    //验证location
    public boolean locationCheck(String location){
        if (location.equals(config.getBumperCar())||location.equals(config.getToyCar())||location.equals(config.getArArea())) {
            return true;
        }
        return false;
    }

    //开启视频录制
    public boolean saveAreaCamera(String place) {
        if (place.equals(config.getBumperCar())) {
            for (DeviceInfo deviceInfo : bumperCarDeviceList) {
                if (!loginAndPreview(deviceInfo, place)) {
                    return false;
                }
            }
            return true;
        } else if (place.equals(config.getToyCar())) {
            if(loginAndPreview(toyCarDevice, place)){
                return true;
            }

        } else if (place.equals(config.getArArea())) {
            if(loginAndPreview(arAreaDevice, place)){
                return true;
            }
        }
        return false;
    }

    public boolean loginAndPreview(DeviceInfo deviceInfo, String place) {
        if (!hcsdkHandler.loginCamera(deviceInfo)) {
            logger.error(place + " 摄像机 " + deviceInfo.getDeviceIp() + " 摄像头注册失败");
            return false;
        }
        String savePath = config.getAreaVideoPath() + deviceInfo.getDeviceIp();
        String videoName = System.currentTimeMillis() + place+".tmp";
        ExpDataCallBack expDataCallBack = new ExpDataCallBack(savePath, videoName);
        videoName = videoName.replace(".tmp", ".mp4");
        String url =savePath+"/"+videoName;
        List<String> visitorIdList = getVisitorIdList(place);
        pathVisitorIdMap.put(url,visitorIdList);
        logger.info("开始录制体验区 "+place+" 的视频");
        Thread task = new Thread(() -> areaCameraPreview(deviceInfo, expDataCallBack));
        task.setDaemon(true);
        areaTaskPool.execute(task);
        return true;
    }

    //体验区视频预览
    public void areaCameraPreview(DeviceInfo deviceInfo, ExpDataCallBack expDataCallBack) {
        logger.info("开启体验区摄像头预览:" + deviceInfo.getDeviceIp());
        NativeLong lRealPlayHandle_area = hcsdkHandler.preView(deviceInfo, expDataCallBack);
        try {
            //体验区视频最大录制时长
            Thread.sleep(config.getAreaVideoMaxTime());
            if (hcsdkHandler.hCNetSDK.NET_DVR_StopRealPlay(lRealPlayHandle_area)) {
                logger.info("摄像机 " + deviceInfo.getDeviceIp() + " 录制视频线程关闭");
            }
            expDataCallBack.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //体验区视频地址入库
    public boolean insertAreaVideoPath(String place,String videoUrl,List<String> visitorIdList) {
        if (visitorIdList.size() == 0) {
            return false;
        }
        for (String visitorId : visitorIdList) {
            mongoDB.saveVideoUrlList(visitorId, place, videoUrl);
        }
        return true;
    }

    //初始化体验区摄像机
    public void setAreaDevice() {
        List<String> areaCameraIpList = config.getBumperCarCameraIps();
        for (String ipStr : areaCameraIpList) {
            DeviceInfo deviceInfo = new DeviceInfo(ipStr, config.getAreaCameraUsername(), config.getAreaCameraPassword(), config.getAreaCameraPort(), 0);
            bumperCarDeviceList.add(deviceInfo);
        }
        toyCarDevice = new DeviceInfo(config.getToyCarCameraIp(), config.getAreaCameraUsername(), config.getAreaCameraPassword(), config.getAreaCameraPort(), 0);
        arAreaDevice = new DeviceInfo(config.getArAreaCameraIp(), config.getAreaCameraUsername(), config.getAreaCameraPassword(), config.getAreaCameraPort(), 0);
    }

}