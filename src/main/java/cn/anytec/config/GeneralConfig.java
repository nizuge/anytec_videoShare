package cn.anytec.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneralConfig {

    @Value("${config.network_segment}")
    private String network_segment;
    @Value("${config.bgm.waterSlide}")
    private String waterSlideBgm;
    @Value("${config.bgm.bumperCar}")
    private String bumperCarBgm;
    @Value("${config.bgm.toyCar}")
    private String toyCarBgm;
    @Value("${config.bgm.arArea}")
    private String arAreaBgm;
    @Value("${config.bgm.path}")
    private String bgmPath;
    @Value("${config.io_module.ip}")
    private String io_module_ip;
    @Value("${config.io_module.port}")
    private short getIo_module_port;
    @Value("${config.io_module.delay}")
    private long io_module_delay;


    @Value("${config.local_save}")
    private boolean local_save;
    @Value("${config.db_insert}")
    private boolean db_insert;
    //camera
    @Value("${camera.gate.ip}")
    private String gateCameraIp;
    @Value("${camera.gate.username}")
    private String gateCameraUsername;
    @Value("${camera.gate.password}")
    private String gateCameraPassword;
    @Value("${camera.gate.port}")
    private short gateCameraPort;
    @Value("${camera.close.ip}")
    private String closeCameraIp;
    @Value("${camera.close.username}")
    private String closeCameraUsername;
    @Value("${camera.close.password}")
    private String closeCameraPassword;
    @Value("${camera.close.port}")
    private short closeCameraPort;
    @Value("${camera.far.ip}")
    private String farCameraIp;
    @Value("${camera.far.username}")
    private String farCameraUsername;
    @Value("${camera.far.password}")
    private String farCameraPassword;
    @Value("${camera.far.port}")
    private short farCameraPort;
    @Value("${camera.area.username}")
    private String areaCameraUsername;
    @Value("${camera.area.password}")
    private String areaCameraPassword;
    @Value("${camera.area.port}")
    private short areaCameraPort;
    //video
    @Value("${video.path}")
    private String videoContext;
    @Value("${video.areaVideoPath}")
    private String areaVideoPath;
    @Value("${video.save}")
    private String videoSavePath;
    @Value("${video.bumperCarCameraIps}")
    private String bumperCarCameraIps;
    @Value("${video.toyCarCameraIp}")
    private String toyCarCameraIp;
    @Value("${video.arAreaCameraIp}")
    private String arAreaCameraIp;
    @Value("${video.areaVideoMaxTime}")
    private long areaVideoMaxTime;
    @Value("${video.gate.duration}")
    private long gateDuration;
    @Value("${video.close.duration}")
    private long duration0;
    @Value("${video.far.duration}")
    private long duration1;
    @Value("${video.close.delay}")
    private long delay;
    @Value("${video.close.fps}")
    private int fps;
    @Value("${video.far.first.start}")
    private String start1;
    @Value("${video.far.second.start}")
    private String start2;
    @Value("${video.far.first.duration}")
    private String video1Duration;
    @Value("${video.far.second.duration}")
    private String video2Duration;
    @Value("${place.waterSlide}")
    private String waterSlide;
    @Value("${place.bumperCar}")
    private String bumperCar;
    @Value("${place.toyCar}")
    private String toyCar;
    @Value("${place.arArea}")
    private String arArea;
    //aliyun
    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;
    //xuanma
    @Value("${xuanma.add_video}")
    private String xuanma_add_video_url;
    @Value("${xuanma.ready}")
    private long xuanma_ready;



    public String getNetwork_segment() {
        return network_segment;
    }

    public String getVideoContext() {
        return videoContext;
    }

    public String getAreaVideoPath() {
        return areaVideoPath;
    }

    public long getDuration0() {
        return duration0;
    }

    public long getDuration1() {
        return duration1;
    }

    public long getDelay() {
        return delay;
    }

    public String getGateCameraIp() {
        return gateCameraIp;
    }

    public String getGateCameraUsername() {
        return gateCameraUsername;
    }

    public String getGateCameraPassword() {
        return gateCameraPassword;
    }

    public short getGateCameraPort() {
        return gateCameraPort;
    }

    public String getCloseCameraIp() {
        return closeCameraIp;
    }

    public String getCloseCameraUsername() {
        return closeCameraUsername;
    }

    public String getCloseCameraPassword() {
        return closeCameraPassword;
    }

    public short getCloseCameraPort() {
        return closeCameraPort;
    }

    public String getFarCameraIp() {
        return farCameraIp;
    }

    public String getFarCameraUsername() {
        return farCameraUsername;
    }

    public String getFarCameraPassword() {
        return farCameraPassword;
    }

    public short getFarCameraPort() {
        return farCameraPort;
    }

    public String getAreaCameraUsername() {
        return areaCameraUsername;
    }

    public String getAreaCameraPassword() {
        return areaCameraPassword;
    }

    public short getAreaCameraPort() {
        return areaCameraPort;
    }

    public String getStart1() {
        return start1;
    }

    public String getStart2() {
        return start2;
    }

    public long getGateDuration() {
        return gateDuration;
    }

    public String getVideo1Duration() {
        return video1Duration;
    }

    public String getVideo2Duration() {
        return video2Duration;
    }

    public String getWaterSlide() {
        return waterSlide;
    }

    public String getBumperCar() {
        return bumperCar;
    }

    public String getToyCar() {
        return toyCar;
    }

    public String getArArea() {
        return arArea;
    }

    public String getWaterSlideBgm() {
        return waterSlideBgm;
    }

    public String getBumperCarBgm() {
        return bumperCarBgm;
    }

    public String getToyCarBgm() {
        return toyCarBgm;
    }

    public String getArAreaBgm() {
        return arAreaBgm;
    }

    public String getBgmPath() {
        return bgmPath;
    }

    public int getFps() {
        return fps;
    }

    public String getVideoSavePath() {
        return videoSavePath;
    }

    public long getAreaVideoMaxTime() {
        return areaVideoMaxTime;
    }

    public String getIo_module_ip() {
        return io_module_ip;
    }

    public short getGetIo_module_port() {
        return getIo_module_port;
    }

    public List<String> getBumperCarCameraIps() {
        List<String> cameraIpList = new ArrayList<>();
        String[] ips = bumperCarCameraIps.split(",");
        for (String ip : ips) {
            cameraIpList.add(ip);
        }
        return cameraIpList;
    }

    public String getToyCarCameraIp() {
        return toyCarCameraIp;
    }

    public String getArAreaCameraIp() {
        return arAreaCameraIp;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getXuanma_add_video_url() {
        return xuanma_add_video_url;
    }

    public boolean isLocal_save() {
        return local_save;
    }

    public boolean isDb_insert() {
        return db_insert;
    }

    public long getIo_module_delayTime() {
        return io_module_delay;
    }

    public long getXuanma_ready() {
        return xuanma_ready;
    }
}



