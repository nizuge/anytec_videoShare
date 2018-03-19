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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Order(value = 1)
public class MyApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);
    ExecutorService taskPool = Executors.newFixedThreadPool(3);
    private static long timeRecord = 0;

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
