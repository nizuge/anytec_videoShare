package cn.anytec.quadrant.slideway;

import cn.anytec.aliyun.vod.VodAPI;
import cn.anytec.aliyun.vod.VodUpload;
import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.ffmpeg.FFMPEGService;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.util.Utils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.vod.upload.resp.UploadURLStreamResponse;
import com.aliyun.vod.upload.resp.UploadVideoResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    VodUpload vodUpload;
    @Autowired
    VodAPI vodAPI;
    @Autowired
    SlideService slideService;

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
        File finalVideo = null;
        while (true){
            try {
                File[] customers = file.listFiles();
                if(customers == null || customers.length == 0){
                    Thread.sleep(2000);
                    continue;
                }
                d1:for (File customer : customers){
                    Thread.sleep(1000);
                    if(customer.getName().equals(videoDir))
                        continue;
                    if(!customer.isDirectory())
                        continue;
                    String[] tmpVideosName = customer.list();
                    if(tmpVideosName == null||tmpVideosName.length != 4)
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
                        String tmpToMp4 = tmpVideo.getName().replace("temp","mp4");
                        source = new File(customer,tmpVideo.getName());
                        output = new File(customer,tmpToMp4);
                        logger.info("开始生成"+tmpToMp4);
                        if(!ffmpegService.convertVideo(source,output,true)){
                            logger.error("生成视频失败："+tmpToMp4);
                            continue d1;
                        }
                    }
                    //第二步：远景摄像头视频剪切

                    //剪切第一段
                    source = new File(customer,"far.mp4");
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
                    logger.info("开始视频降帧处理");
                    if(!ffmpegService.deferVideo(source,output,config.getCloseFps(),config.getPts(),true)){
                        logger.error("视频降帧处理失败");
                        continue ;
                    }
                    //第四步：创建视频合并列表
                    //合并列表
                    File concatFile1 = new File(file,"concat1.txt");
                    File concatFile2 = new File(file,"concat2.txt");
                    //游客准备视频与滑梯部分视频合并列表
                    File concatFile3 = new File(file,"concat3.txt");

                    File concatFile5 = new File(file,"concat5.txt");
                    File concatFile6 = new File(file,"concat6.txt");

                    StringBuilder concatText1 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("far1.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("slow.mp4").append("'\n");
                    StringBuilder concatText2 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("merge12.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("far2.mp4").append("'\n");
                    StringBuilder concatText3 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("preA.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("gateA.mp4").append("'\n");
                    StringBuilder concatText5 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("prepareA.mp3").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("waterslideA.mp3").append("'\n");
                    StringBuilder concatText6 = new StringBuilder()
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("prepareS.mp4").append("'\n")
                            .append("file '").append(customer.getAbsolutePath()).append(File.separator).append("waterslideS.mp4").append("'\n");
                    OutputStream outputStream1 = null;
                    OutputStream outputStream2 = null;
                    OutputStream outputStream3 = null;
                    OutputStream outputStream5 = null;
                    OutputStream outputStream6 = null;

                    try {
                        outputStream1 = new FileOutputStream(concatFile1);
                        outputStream2 = new FileOutputStream(concatFile2);
                        outputStream3 = new FileOutputStream(concatFile3);
                        outputStream5 = new FileOutputStream(concatFile5);
                        outputStream6 = new FileOutputStream(concatFile6);
                        outputStream1.write(concatText1.toString().getBytes());
                        outputStream2.write(concatText2.toString().getBytes());
                        outputStream3.write(concatText3.toString().getBytes());
                        outputStream5.write(concatText5.toString().getBytes());
                        outputStream6.write(concatText6.toString().getBytes());
                        outputStream1.flush();
                        outputStream2.flush();
                        outputStream3.flush();
                        outputStream5.flush();
                        outputStream6.flush();
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
                            if(outputStream3!=null) {
                                outputStream3.close();
                            }
                            if(outputStream5!=null) {
                                outputStream5.close();
                            }
                            if(outputStream6!=null) {
                                outputStream6.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    source = concatFile1;
                    output = new File(customer,"merge12.mp4");
                    //第五步：合并一二段视频
                    logger.info("开始合并第一二段视频");
                    if(!ffmpegService.concatMedia(source,output,true)){
                        logger.error("合成一二段视频失败");
                        continue;
                    }
                    //第六步：合并二三段视频
                    source = concatFile2;
                    output = new File(customer,"slide.mp4");
                    logger.info("开始合并第三段视频并生成滑梯部分完整视频");
                    if(!ffmpegService.concatMedia(source,output,true)){
                        logger.error("生成滑梯部分完整视频失败");
                        continue;
                    }
                    //第七步：准备视频背景音乐合成
                    logger.info("开始合成第一段准备视频背景音乐");
                    source = new File(customer,"pre.mp4");
                    output = new File(customer,"preA.mp4");
                    File audio_part1 = new File(config.getWaterSlideBgmPart1());
                    if(!ffmpegService.mergeAudio(audio_part1,source,output,true)){
                        logger.info("合成第一段准备视频背景音乐时失败");
                            break ;
                    }
                    logger.info("开始合成第二段准备视频背景音乐");
                    source = new File(customer,"gate.mp4");
                    output = new File(customer,"gateA.mp4");
                    File audio_part2 = new File(config.getWaterSlideBgmPart2());
                    if(!ffmpegService.mergeAudio(audio_part2,source,output,true)){
                        logger.info("合成第二段准备视频背景音乐时失败");
                            break ;
                    }
                    //第八步：两段准备视频合成
                    source = concatFile3;
                    output = new File(customer,"prepare.mp4");
                    logger.info("开始合并一二段的游客准备视频");
                    if(!ffmpegService.concatMedia(source,output,true)){
                        logger.error("生成一二段准备视频失败");
                        continue;
                    }
                    //第九步：playing部分视频背景音乐合成
                    logger.info("开始合成playing部分视频背景音乐");
                    source = new File(customer,"slide.mp4");
                    output = new File(customer,"waterslide.mp4");
                    File audio_play = new File(config.getWaterSlideBgm());
                    if(!ffmpegService.mergeAudio(audio_play,source,output,true)){
                        logger.info("playing部分视频背景音乐合成失败");
                            break ;
                    }
                    //第十步：ready部分媒体音频视频分离
                    source = new File(customer,"prepare.mp4");
                    output = new File(customer,"prepareA.mp3");
                    if(!ffmpegService.separateAudio(source,output,true)){
                        logger.error("ready部分分离出音频失败");
                        continue;
                    }
                    output = new File(customer,"prepareV.mp4");
                    if(!ffmpegService.separateVideo(source,output,true)){
                        logger.error("ready部分分离出视频失败");
                        continue;
                    }
                    //第十一步：playing部分媒体音频视频分离
                    source = new File(customer,"waterslide.mp4");
                    output = new File(customer,"waterslideA.mp3");
                    if(!ffmpegService.separateAudio(source,output,true)){
                        logger.error("playing部分分离出音频失败");
                        continue;
                    }
                    output = new File(customer,"waterslideV.mp4");
                    if(!ffmpegService.separateVideo(source,output,true)){
                        logger.error("playing部分分离出视频失败");
                        continue;
                    }
                    //第十二步：ready 与 playing部分视频同帧率处理
                    logger.info("开始进行ready与playing部分视频同帧率处理");
                    source = new File(customer,"prepareV.mp4");
                    output = new File(customer,"prepareS.mp4");
                    if(!ffmpegService.changeFps(source,output,config.getFps(),true)){
                        logger.error(source.getName()+"变帧处理失败");
                        continue;
                    }
                    source = new File(customer,"waterslideV.mp4");
                    output = new File(customer,"waterslideS.mp4");
                    if(!ffmpegService.changeFps(source,output,config.getFps(),true)){
                        logger.error(source.getName()+"变帧处理失败");
                        continue;
                    }
                    //第十三步：ready与playing部分分别合成音频视频
                    source = concatFile5;
                    output = new File(customer,"final.mp3");
                    if(!ffmpegService.concatMedia(source,output,true)){
                        logger.error("完整音频合并失败");
                        continue;
                    }
                    source = concatFile6;
                    output = new File(customer,"final.mp4");
                    if(!ffmpegService.concatMedia(source,output,true)){
                        logger.error("完整视频合并失败");
                        continue;
                    }
                    //最终视频存储位置获取
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
                    finalVideo = new File(location.toString());
                    //第十四步：合并整个视频
                    File audio = new File(customer,"final.mp3");
                    File video = new File(customer,"final.mp4");
                    logger.info("开始最终合成");
                    if(!ffmpegService.mergeAudio(audio,video,finalVideo,true)){
                        logger.error("最终合成失败");
                        continue;
                    }

                    if(!Utils.clearDir(customer)){
                        logger.error("删除游客"+customer.getName()+"视频处理文件夹失败");
                    }
                    logger.info("========== 视频生成完毕 ==========");
                    if(config.isDb_insert()){
                        StringBuilder url = new StringBuilder("http://")
                                .append(listener.getHostIP()).append(":").append(listener.getPort())
                                .append("/anytec/videos/").append(customer.getName())
                                .append(File.separator).append(videoName);
                        mongoDB.saveVideoUrlList(customer.getName(),config.getWaterSlide(),url.toString());
                    }
                    logger.info("开始视频上传");
                    UploadVideoResponse response = vodUpload.uploadVideo(customer.getName()+"——"+System.currentTimeMillis(),finalVideo.getAbsolutePath(),null,null,null,null,null,null);
                    if(response.isSuccess()){
                        logger.info("视频上传成功");
                        if(!config.isLocal_save()){
                            if( !Utils.clearDir(finalVideo.getParentFile())){
                                logger.error("删除源视频失败");
                            }
                        }
                        String videoId = response.getVideoId();
                        Map<String,String> params = new HashMap<>();
                        params.put("Action","GetPlayInfo");
                        params.put("VideoId",videoId);
                        params.put("Formats","mp4");
                        Thread.sleep(10000);
                        int getPlayInfoTimes = 0;
                        JSONObject reply = null;
                        while(getPlayInfoTimes < 8) {
                            getPlayInfoTimes++;
                            try {
                                reply = JSONObject.parseObject(vodAPI.requestAPI(params));
                                break ;
                            } catch (Exception e) {
                                logger.error("获取阿里云视频点播地址时失败,等待再次请求");
                                Thread.sleep(1500);
                                if (getPlayInfoTimes == 8) {
                                    logger.error("获取阿里云视频点播地址时失败:" + e.getMessage());
                                    break d1;
                                }
                                e.printStackTrace();
                            }
                        }
                        if(reply != null && reply.containsKey("PlayInfoList")){
                            /*JSONObject videoBase = reply.getJSONObject("VideoBase");
                            String coverUrl = videoBase.getString("CoverURL");
                            if(coverUrl==null){
                                coverUrl="";
                            }
                            logger.info("视频封面图片地址：" + coverUrl);*/
                            JSONArray aliVideoList = reply.getJSONObject("PlayInfoList").getJSONArray("PlayInfo");
                            logger.info("视频数量："+aliVideoList.size());
                            for (int i = 0; i < aliVideoList.size(); i++) {
                                JSONObject aliVideo = aliVideoList.getJSONObject(i);
                                String format = aliVideo.getString("Format");
                                logger.info(i+1+"--视频格式："+format);
                                String videoPlayUrl = aliVideo.getString("PlayURL");
                                logger.info(i+1+"--视频URL："+videoPlayUrl);
                                String coverUrl = videoPlayUrl+"?x-oss-process=video/snapshot,t_6000,f_jpg,w_800,h_600";
                                logger.info("视频封面图片地址：" + coverUrl);
                                HttpResponse httpResponse;
                                HttpEntity entity;
                                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                                multipartEntityBuilder.addTextBody("cover_url", coverUrl);
                                logger.info("炫马接口 —— cover_url : " + coverUrl);
                                multipartEntityBuilder.addTextBody("url",videoPlayUrl);
                                logger.info("炫马接口 —— url : "+videoPlayUrl);
                                JSONArray userIds = new JSONArray();
                                userIds.add(customer.getName());
                                multipartEntityBuilder.addTextBody("user_id",userIds.toJSONString());
                                logger.info("炫马接口 —— user_id : "+customer.getName());
                                multipartEntityBuilder.addTextBody("location",config.getWaterSlide());
                                logger.info("炫马接口 —— location : "+config.getWaterSlide());
                                entity = multipartEntityBuilder.build();
                                try {
                                    httpResponse = Request.Post(config.getXuanma_add_video_url())
                                            .connectTimeout(10000)
                                            .socketTimeout(30000)
                                            .body(entity)
                                            .execute().returnResponse();
                                    try {
                                        JSONObject xmreply = JSONObject.parseObject(EntityUtils.toString(httpResponse.getEntity()));
                                        if(xmreply != null && xmreply.getInteger("code")==100){
                                            logger.info(" 游客"+customer.getName()+"视频分享完成");
                                        }else {
                                            logger.error(xmreply.toJSONString());
                                            logger.error("请求炫马视频添加接口失败,user_id="+customer.getName()+",location="+config.getWaterSlide()+",url="+videoPlayUrl);
                                        }
                                    }catch (Exception e){
                                        logger.error("解析炫马接口响应json时发生错误:"+e.getMessage());
                                        logger.error("请求炫马视频添加接口失败,user_id="+customer.getName()+",location="+config.getWaterSlide()+",url="+videoPlayUrl);
                                    }

                                } catch (ClientProtocolException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }else {
                        if(config.isLocal_save()){
                            logger.error("上传视频到阿里云失败,视频本地地址:"+finalVideo.getAbsolutePath());
                        }else {
                            logger.error("上传视频到阿里云失败,游客id:"+customer.getName());
                        }
                        continue;
                    }
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
