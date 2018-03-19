package cn.anytec.quadrant.expZone;

import cn.anytec.config.GeneralConfig;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import cn.anytec.util.RuntimeLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ExpVideoProcessing implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ExpVideoProcessing.class);

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    MongoDBService mongoDB;

    /**
     * 目录及文件创建，视频处理
     */
    @Override
    public void run() {
        String areaVideoPath = config.getAreaVideoPath();
        File areaFile = new File(areaVideoPath);
        if (!areaFile.exists()) {
            if (!areaFile.mkdirs()) {
                logger.error("存储体验区视频文件的文件夹不存在，创建文件夹失败");
                return;
            }
        }
        while (true) {
            try {
                File[] areaVideoFlies = areaFile.listFiles();
                if (areaVideoFlies == null) {
                    Thread.sleep(2000);
                    continue;
                }
                //开始处理视频
                for (File cameraIpFile : areaVideoFlies) {
                    String[] areaVideos = cameraIpFile.list();
                    if(areaVideos.length!=0){
                        for (String areaVideo : areaVideos) {
                            if (!areaVideo.contains("areaVideo.temp")) {
                                continue;
                            }
                            logger.info("========== 视频处理 ==========");
                            RuntimeLocal runtimeLocal = new RuntimeLocal();
                            String areaVideoName = areaVideo.split("\\.")[0] + ".mp4";
                            File source = new File(cameraIpFile,areaVideo);
                            File output = new File(cameraIpFile,areaVideoName);
                            String[] makeAreaVideo = new String[]{
                                    "ffmpeg","-i",source.getAbsolutePath(),
                                    "-strict","-2",
                                    "-y",output.getAbsolutePath()
                            };
                            logger.info("开始生成" + areaVideoName);
                            logger.debug(runtimeLocal.execute(makeAreaVideo));
                            new File(cameraIpFile,areaVideo).delete();
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("视频处理时出现异常");
                e.printStackTrace();
                return;
            }
        }
    }
}
