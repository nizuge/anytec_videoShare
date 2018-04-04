package cn.anytec.config;
/**
 * 服务器启动时自动执行
 */
import cn.anytec.quadrant.expZone.ExpVideoProcessing;
import cn.anytec.quadrant.expZone.ExpZoneService;
import cn.anytec.quadrant.hcEntity.DeviceInfo;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import cn.anytec.quadrant.slideway.SlideService;
import cn.anytec.quadrant.slideway.SlideVideoProcessing;
import cn.anytec.util.RuntimeLocal;
import cn.anytec.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Order(value = 1)
public class MyApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);
    ExecutorService taskPool = Executors.newFixedThreadPool(3);
    private static long timeRecord = 0;
    private static final String pro = "server:\n" +
            "  port: 9999\n" +
            "config:\n" +
            "  #服务器网段\n" +
            "  network_segment: 192.168.0\n" +
            "  #IO模块\n" +
            "  io_module:\n" +
            "    ip: 192.168.0.226\n" +
            "    port: 502\n" +
            "    #2s内不能连续触发\n" +
            "    interval: 2000\n" +
            "  bgm: /home/anytec/videoShare/test.mp3\n" +
            "  local_save: true\n" +
            "  db_insert: true\n" +
            "#炫马接口\n" +
            "xuanma:\n" +
            "  add_video: http://wechat.xuanma.tech/index/deviceapi/add_video\n" +
            "#阿里云视频点播\n" +
            "aliyun:\n" +
            "  accessKeyId: HA3Dj8EUaJ6ODtEg\n" +
            "  accessKeySecret: ARmCIra7FCWl8LGAAxG7KOVAzZzd6D\n" +
            "video:\n" +
            "  #滑梯各段视频保存路径\n" +
            "  path: /home/anytec/videoShare/slideway/\n" +
            "  #滑梯合成视频保存路径\n" +
            "  save: /home/anytec/videoShare/composite/generateVideo/\n" +
            "  #体验区视频保存路径\n" +
            "  areaVideoPath: /home/anytec/videoShare/videoArea/\n" +
            "  #水上滑梯摄像头Ip地址\n" +
            "  bumperCarCameraIps: 192.168.0.222,192.168.0.223\n" +
            "  toyCarCameraIp: 192.168.0.224\n" +
            "  arAreaCameraIp: 192.168.0.225\n" +
            "  #体验区视频最大拍摄时间（s）\n" +
            "  areaVideoMaxTime: 15000\n" +
            "  #近景视频\n" +
            "  close:\n" +
            "  #拍摄时长\n" +
            "    duration: 2000\n" +
            "  #远景摄像头启动后延迟2s开始拍摄\n" +
            "    delay: 2000\n" +
            "  #放慢处理后的视频帧数\n" +
            "    fps: 10\n" +
            "  #远景视频\n" +
            "  far:\n" +
            "  #拍摄时长\n" +
            "    duration: 6000\n" +
            "  #落水前视频\n" +
            "    first:\n" +
            "  #开始拍摄时间\n" +
            "      start: 00:00:00\n" +
            "  #拍摄时长（s）\n" +
            "      duration: 00:00:2.2\n" +
            "  #落水后视频\n" +
            "    second:\n" +
            "  #开始拍摄时间\n" +
            "      start: 00:00:4.0\n" +
            "  #拍摄时长\n" +
            "      duration: 00:00:3\n" +
            "camera:\n" +
            "  #近景摄像机\n" +
            "  close:\n" +
            "    ip: 192.168.0.220\n" +
            "    username: admin\n" +
            "    password: n-tech123\n" +
            "    port: 8000\n" +
            "  #远景摄像机\n" +
            "  far:\n" +
            "    ip: 192.168.0.221\n" +
            "    username: admin\n" +
            "    password: n-tech123\n" +
            "    port: 8000\n" +
            "  area:\n" +
            "    username: admin\n" +
            "    password: n-tech123\n" +
            "    port: 8000\n" +
            "place:\n" +
            "  waterSlide: waterSlide\n" +
            "  bumperCar: bumperCar\n" +
            "  toyCar: toyCar\n" +
            "  arArea: arArea";

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    SlideVideoProcessing slideVideoProcessing;
    @Autowired
    ExpVideoProcessing expVideoProcessing;
    @Autowired
    SlideService slideService;
    @Autowired
    ExpZoneService expZoneService;

    private Socket socket;


    @Override
    public void run(ApplicationArguments arg) throws Exception {
        logger.info("====== 创建配置文件 ======");
        File ini = new File("config");
        if(!ini.exists()){
            if(!ini.mkdir()){
                logger.error("检查工程根目录及配置文件");
                System.exit(1);
            }
        }
        File properties = new File(ini,"application-dev.yml");
        if(!properties.exists()){
            OutputStream outputStream = new FileOutputStream(properties);
            outputStream.write(pro.getBytes());
            outputStream.flush();
            outputStream.close();
        }
        logger.info("====== ffmpeg 检查 ======");
        File check = new File("/usr/bin/ffmpeg");
        if(!check.exists()){
            logger.error("Check if ffmpeg is installed");
            System.exit(1);
        }
        logger.info("====== 远近景摄像头设置 =======");
        slideService.setCloseView(new DeviceInfo(config.getCloseCameraIp(), config.getCloseCameraUsername(), config.getCloseCameraPassword(), config.getCloseCameraPort(), 0));
        slideService.setFarView(new DeviceInfo(config.getFarCameraIp(), config.getFarCameraUsername(), config.getFarCameraPassword(), config.getFarCameraPort(), 1));
        expZoneService.setAreaDevice();

        logger.info("====== 启动视频处理线程 =======");
        Thread vthread = new Thread(slideVideoProcessing);
        vthread.setDaemon(true);
        Thread.sleep(2000);
        vthread.start();

        logger.info("====== 启动体验区视频处理线程 =======");
        Thread areaThread = new Thread(expVideoProcessing);
        areaThread.setDaemon(true);
        Thread.sleep(2000);
        areaThread.start();

        logger.info("====== 启动时与IO模块进行Socket连接 =======");
        while (true){
            Thread thread = null;
            try {
                socket = buildSocket();
                if(socket == null||!socket.isConnected())
                    throw new Exception("Socket连接失败");
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStream inputStream = null;
                        try{
                            inputStream = socket.getInputStream();
                            if(inputStream == null){
                                logger.error("连接失败！");
                                return;
                            }
                            while (true){
                                byte[] b = new byte[10];
                                inputStream.read(b);
                                String info = Utils.bytesToHexString(b);
                                logger.info("IO模块："+info);
                                parseIO(info);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            if(inputStream != null){
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                thread.setDaemon(true);
                thread.start();
                while (true){
                    socket.sendUrgentData(0xFF);
                    Thread.sleep(1000);
                }

            }catch (Exception e){
                logger.error(e.getMessage());
                if(socket != null)
                    socket.close();
                if(thread != null && thread.isAlive())
                    thread.interrupt();
                Thread.sleep(1000);
            }
        }

    }



    /**
     * 建立socket客户端连接
     * @return
     */
    private Socket buildSocket(){

        Socket socket = null;
        try {
            socket = new Socket(config.getIo_module_ip(),config.getGetIo_module_port());
            socket.setKeepAlive(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    /**
     * IO模块信号解析
     * @param info
     */
    private void parseIO(String info){
        String DI = info.substring(16);
        long currentTime = System.currentTimeMillis();
        if(!DI.equals("0100") && currentTime-timeRecord > config.getGetIo_module_interval()){
            timeRecord = currentTime;
            logger.info("开始录制视频");
            Thread task = new Thread(()-> slideService.notifySlide());
            task.setDaemon(true);
            taskPool.execute(task);
        }
    }
}
