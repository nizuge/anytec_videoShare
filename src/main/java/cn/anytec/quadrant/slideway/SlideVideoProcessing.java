package cn.anytec.quadrant.slideway;

import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.ffmpeg.FFMPEGService;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SlideVideoProcessing implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(SlideVideoProcessing.class);
    private static final String videoDir = "generateVideo";

    @Autowired
    GeneralConfig config;
    @Autowired
    MongoDBService mongoDB;
    @Autowired
    SpringBootListener listener;
    @Autowired
    FFMPEGService ffmpegService;

    /**
     * 目录及文件创建，视频处理
     */
    @Override
    public void run() {
        String root = config.getVideoContext();
        File file = new File(root);
        if(!file.exists()){
            if(!file.mkdirs()){
                logger.error("存储临时视频文件的文件夹不存在，创建文件夹失败");
                System.exit(1);
            }
        }
        File saveDir = new File(config.getVideoSavePath());
        if(!saveDir.exists()){
            if(!saveDir.mkdirs()){
                logger.error("创建视频存储文件夹失败");
                return;
            }
        }
        while (true){
            File finalVideo = null;
            try {
                File[] customers = file.listFiles();
                if(customers == null || customers.length == 0){
                    Thread.sleep(2000);
                    continue;
                }
                d1:for (File customer : customers){
                    if(customer.getName().equals(videoDir))
                        continue;
                    if(!customer.isDirectory())
                        continue;
                    String[] tmpVideosName = customer.list();
                    if(tmpVideosName == null||tmpVideosName.length != 2)
                        continue;
                    for(String tmpVideoName:tmpVideosName){
                        if(!tmpVideoName.contains("temp"))
                            continue d1;
                    }
                    //开始处理视频
                    logger.info("========== 视频处理 ==========");
                    File source;
                    File output;
                    //第一步：将临时的temp文件编码为MP4视频文件并删除临时文件
                    for(File tmpVideo : customer.listFiles()){
                        String tmpToMp4;
                        if(tmpVideo.getName().equals("close.temp")){
                            tmpToMp4 = "close.mp4";
                        }else {
                            tmpToMp4 = "far.mp4";
                        }
                        source = new File(customer,tmpVideo.getName());
                        output = new File(customer,tmpToMp4);
                        logger.info("开始生成"+tmpToMp4);
                        if(!ffmpegService.convertVideo(source,output,true)){
                            logger.error("生成视频失败："+tmpToMp4);
                            continue d1;
                        }
                    }
                    //第二步：远景摄像头视频剪切
                    //预处理
                    source = new File(customer,"far.mp4");
                    output = new File(customer,"cut.mp4");
                    logger.info("视频剪切准备");
                    if(!ffmpegService.preVideo(source,output,true)){
                        logger.error("视频剪切预处理失败");
                        continue;
                    }
                    //剪切第一段
                    source = new File(customer,"cut.mp4");
                    output = new File(customer,"far1.mp4");
                    logger.info("开始剪切第一段远景摄像头视频");
                    if(!ffmpegService.cutVideo(source,output,config.getStart1(),config.getVideo1Duration(),true)){
                        logger.error("视频剪切失败");
                        continue;
                    }
                    //剪切第二段
                    output = new File(customer,"far2.mp4");
                    logger.info("开始剪切第二段远景摄像头视频");
                    if(!ffmpegService.cutVideo(source,output,config.getStart2(),config.getVideo2Duration(),true)){
                        logger.error("视频剪切失败");
                        continue;
                    }
                    //第三步：放慢处理近景视频
                    source = new File(customer,"close.mp4");
                    output = new File(customer,"slow.mp4");
                    logger.info("视频降帧处理");
                    if(!ffmpegService.deferVideo(source,output,config.getFps(),true)){
                        logger.error("视频降帧处理失败");
                    }
                    //第四步：创建视频合并列表
                    //合并列表
                    File concatFile1 = new File(file,"concat1.txt");
                    File concatFile2 = new File(file,"concat2.txt");

                    StringBuilder concatText1 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("far1.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("slow.mp4").append("'\n");
                    StringBuilder concatText2 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("merge12.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("far2.mp4").append("'\n");
                    OutputStream outputStream1 = null;
                    OutputStream outputStream2 = null;

                    try {
                        outputStream1 = new FileOutputStream(concatFile1);
                        outputStream2 = new FileOutputStream(concatFile2);
                        outputStream1.write(concatText1.toString().getBytes());
                        outputStream2.write(concatText2.toString().getBytes());
                        outputStream1.flush();
                        outputStream2.flush();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if(outputStream1!=null) {
                                outputStream1.close();
                            }
                            if(outputStream2!=null) {
                                outputStream2.close();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    source = concatFile1;
                    output = new File(customer,"merge12.mp4");
                    //第五步：合并一二段视频
                    logger.info("开始合并第一二段视频");
                    if(!ffmpegService.concatVideos(source,output,true)){
                        logger.error("合成一二段视频失败");
                        continue;
                    }
                    //第六步：合并二三段视频

                    source = concatFile2;
                    output = new File(customer,"an.mp4");
                    logger.info("开始合并第三段视频并生成完整视频");
                    if(!ffmpegService.concatVideos(source,output,true)){
                        logger.error("生成完整视频失败");
                        continue;
                    }

                    StringBuilder location = new StringBuilder(config.getVideoSavePath())
                            .append(customer.getName());
                    File dir = new File(location.toString());
                    if(!dir.exists()){
                        if(!dir.mkdirs()){
                            logger.error("创建游客视频存储文件夹失败！");
                            continue;
                        }
                    }
                    String videoName = System.currentTimeMillis()+".mp4";
                    location.append(File.separator).append(videoName);
                    File audio = new File(config.getBgm());
                    File video = output;
                    finalVideo = new File(location.toString());
                    concatFile1.delete();
                    concatFile2.delete();
                    //添加音频
                    logger.info("开始合成背景音乐");
                    if(!ffmpegService.mergeAudio(audio,video,finalVideo,true)){
                        logger.error("合成音频失败");
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = new FileInputStream(video);
                            os = new FileOutputStream(finalVideo);
                            byte[] anVideo = Utils.streamToByte(is);
                            if(anVideo != null){
                                os.write(anVideo);
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
                    StringBuilder url = new StringBuilder("http://")
                            .append(listener.getHostIP()).append(":").append(listener.getPort())
                            .append("/anytec/videos/").append(customer.getName())
                            .append(File.separator).append(videoName);
                    List<String> urllist = new ArrayList<>();
                    urllist.add(url.toString());
                    mongoDB.saveVideoUrlList(customer.getName(),config.getWaterSlide(),urllist);
                    logger.info("=========== 完成 ===========");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e){
                logger.error("视频处理时出现异常");
                if(finalVideo != null && !finalVideo.isDirectory())
                    finalVideo.delete();
                e.printStackTrace();
            }

        }

    }
}
