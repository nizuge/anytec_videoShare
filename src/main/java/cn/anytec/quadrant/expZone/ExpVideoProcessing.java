package cn.anytec.quadrant.expZone;

import cn.anytec.aliyun.vod.VodAPI;
import cn.anytec.aliyun.vod.VodUpload;
import cn.anytec.config.GeneralConfig;
import cn.anytec.ffmpeg.FFMPEGService;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import cn.anytec.util.RuntimeLocal;
import cn.anytec.util.Utils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.vod.upload.resp.UploadVideoResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExpVideoProcessing implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ExpVideoProcessing.class);

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    MongoDBService mongoDB;
    @Autowired
    ExpZoneService expZoneService;
    @Autowired
    VodUpload vodUpload;
    @Autowired
    VodAPI vodAPI;
    @Autowired
    FFMPEGService ffmpegService;


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
                    if (areaVideos.length != 0) {
                        for (String areaVideo : areaVideos) {
                            if (!areaVideo.contains(".temp")) {
                                continue;
                            }
                            logger.info("========== 视频处理 ==========");
                            RuntimeLocal runtimeLocal = new RuntimeLocal();
                            String areaVideoName = areaVideo.split("\\.")[0] + ".mp4";
                            String location = "";
                            File audio =null;
                            if (areaVideoName.contains(config.getBumperCar())) {
                                location = config.getBumperCar();
                                audio =new File(config.getBumperCarBgm());
                            } else if (areaVideoName.contains(config.getToyCar())) {
                                location = config.getToyCar();
                                audio =new File(config.getToyCarBgm());
                            } else if (areaVideoName.contains(config.getArArea())) {
                                location = config.getArArea();
                                audio =new File(config.getArAreaBgm());
                            } else {
                                File errorFile = new File(cameraIpFile, areaVideo);
                                errorFile.delete();
                                continue;
                            }
                            File source = new File(cameraIpFile, areaVideo);
                            File output = new File(cameraIpFile, "tmp"+areaVideoName);
                            String[] makeAreaVideo = new String[]{
                                    "ffmpeg", "-i", source.getAbsolutePath(),
                                    "-strict", "-2",
                                    "-y", output.getAbsolutePath()
                            };
                            logger.info("开始生成" + "tmp"+areaVideoName);
                            logger.debug(runtimeLocal.execute(makeAreaVideo));
                            if (output.exists()) {
                                new File(cameraIpFile, areaVideo).delete();
                                logger.info("tmp"+areaVideoName + "视频生成成功！");
                                logger.info("开始合成背景音乐");
                                File finalVideo = new File(cameraIpFile, areaVideoName);
                                if(!ffmpegService.mergeAudio(audio,output,finalVideo,true)){
                                    logger.error("合成音频失败");
                                    InputStream is = null;
                                    OutputStream os = null;
                                    try {
                                        is = new FileInputStream(output);
                                        os = new FileOutputStream(finalVideo);
                                        byte[] anVideo = Utils.streamToByte(is);
                                        if(anVideo != null){
                                            os.write(anVideo);
                                            os.flush();
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
                                Map<String, List<String>> pathVisitorIdMap = expZoneService.getPathVisitorIdMap();
                                String idJson = "";
                                if (pathVisitorIdMap.containsKey(finalVideo.getAbsolutePath())) {
                                    List<String> visitorIdList = pathVisitorIdMap.get(finalVideo.getAbsolutePath());
                                    if (visitorIdList.size() > 0) {
                                        idJson = JSON.toJSONString(visitorIdList);
                                    }
                                    output.delete();
                                    logger.info("合成成功,开始视频上传");
                                    UploadVideoResponse response = vodUpload.uploadVideo(finalVideo.getName(), finalVideo.getAbsolutePath(), null, null, null, null, null, null);
                                    if (response.isSuccess()) {
                                        logger.info("视频上传成功");
                                        String videoId = response.getVideoId();
                                        Map<String, String> params = new HashMap<>();
                                        params.put("Action", "GetPlayInfo");
                                        params.put("VideoId", videoId);
                                        params.put("Formats", "mp4");
                                        Thread.sleep(10000);
                                        int getPlayInfoTimes = 0;
                                        JSONObject reply = null;
                                        while (getPlayInfoTimes < 8) {
                                            getPlayInfoTimes++;
                                            try {
                                                reply = JSONObject.parseObject(vodAPI.requestAPI(params));
                                                break;
                                            } catch (Exception e) {
                                                logger.error("获取阿里云视频点播地址时失败,等待再次请求");
                                                Thread.sleep(1500);
                                                if (getPlayInfoTimes == 8) {
                                                    logger.error("获取阿里云视频点播地址时失败:" + e.getMessage());
                                                    break;
                                                }
                                                e.printStackTrace();
                                            }
                                        }
                                        if (reply != null && reply.containsKey("PlayInfoList")) {
                                            /*JSONObject videoBase = reply.getJSONObject("VideoBase");
                                            String coverUrl = videoBase.getString("CoverURL");
                                            if(coverUrl==null){
                                                coverUrl="";
                                            }
                                            logger.info("视频封面图片地址：" + coverUrl);*/
                                            JSONArray aliVideoList = reply.getJSONObject("PlayInfoList").getJSONArray("PlayInfo");
                                            logger.info("视频数量：" + aliVideoList.size());
                                            for (int i = 0; i < aliVideoList.size(); i++) {
                                                JSONObject aliVideo = aliVideoList.getJSONObject(i);
                                                String format = aliVideo.getString("Format");
                                                logger.info(i + 1 + "--视频格式：" + format);
                                                String videoPlayUrl = aliVideo.getString("PlayURL");
                                                logger.info(i + 1 + "--视频URL：" + videoPlayUrl);
                                                String coverUrl = videoPlayUrl+"?x-oss-process=video/snapshot,t_6000,f_jpg,w_800,h_600";
                                                logger.info("视频封面图片地址：" + coverUrl);
                                                if (config.isDb_insert()) {
                                                    expZoneService.insertAreaVideoPath(location, videoPlayUrl, visitorIdList);
                                                }
                                                logger.info("体验区视频成功入库本地mongodb");
                                                pathVisitorIdMap.remove(finalVideo.getAbsolutePath());
                                                HttpResponse httpResponse;
                                                HttpEntity entity;
                                                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                                                multipartEntityBuilder.addTextBody("cover_url", coverUrl);
                                                logger.info("炫马接口 —— cover_url : " + coverUrl);
                                                multipartEntityBuilder.addTextBody("url", videoPlayUrl);
                                                logger.info("炫马接口 —— url : " + videoPlayUrl);
                                                multipartEntityBuilder.addTextBody("user_id", idJson);
                                                logger.info("炫马接口 —— user_id : " + idJson);
                                                multipartEntityBuilder.addTextBody("location", location);
                                                logger.info("炫马接口 —— location : " + location);
                                                entity = multipartEntityBuilder.build();
                                                try {
                                                    httpResponse = Request.Post(config.getXuanma_add_video_url())
                                                            .connectTimeout(10000)
                                                            .socketTimeout(30000)
                                                            .body(entity)
                                                            .execute().returnResponse();
                                                    JSONObject xmreply = JSONObject.parseObject(EntityUtils.toString(httpResponse.getEntity()));
                                                    if (xmreply != null && xmreply.getInteger("code") == 100) {
                                                        logger.info(finalVideo.getName() + "视频分享完成");
                                                    } else {
                                                        logger.error("请求炫马视频添加接口失败," + "location = " + location + ", url = " + videoPlayUrl);
                                                    }
                                                } catch (ClientProtocolException e) {
                                                    e.printStackTrace();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    } else {
                                        if (config.isLocal_save()) {
                                            logger.error("上传视频到阿里云失败,视频本地地址:" + finalVideo.getAbsolutePath());
                                        } else {
                                            logger.error("上传视频到阿里云失败,视频本地地址:" + finalVideo.getAbsolutePath());
                                        }
                                        continue;
                                    }
                                }
                            }
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
