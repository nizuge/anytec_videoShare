package cn.anytec.controller;

import cn.anytec.aliyun.vod.VodAPI;
import cn.anytec.aliyun.vod.VodUpload;
import cn.anytec.config.GeneralConfig;

import cn.anytec.config.MyApplicationRunner;
import cn.anytec.ffmpeg.FFMPEGService;
import cn.anytec.ffmpeg.FewMediaInfo;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.quadrant.expZone.ExpZoneService;
import cn.anytec.quadrant.slideway.SlideService;
import cn.anytec.util.Utils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.vod.upload.resp.UploadVideoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController{

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    GeneralConfig generalConfig;
    @Autowired
    MongoDBService mongoDB;
    @Autowired
    FFMPEGService ffmpegService;
    @Autowired
    SlideService slideService;
    @Autowired
    ExpZoneService expZoneService;
    @Autowired
    VodAPI vodAPI;
    @Autowired
    VodUpload vodUpload;
    @Autowired
    MyApplicationRunner myApplicationRunner;

    //根据Id和地点获取视频地址
    @RequestMapping(value = "/anytec/videos",method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getVideoList(@RequestParam("id")String visitorId,@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/videos");
        logger.info("参数1：id="+visitorId);
        logger.info("参数2：location="+place);
        Map<String,Object> resultMap = new HashMap<>();
        if(!generalConfig.isDb_insert()){
            resultMap.put("code","database disabled");
            response.setStatus(400);
            return new JSONObject(resultMap).toJSONString();
        }
        if(!expZoneService.locationCheck(place) && !place.equals(generalConfig.getWaterSlide())){
            resultMap.put("code","bad location");
            response.setStatus(400);
            return new JSONObject(resultMap).toJSONString();
        }
        resultMap.put("location",place);
        resultMap.put("id",visitorId);
        List<String> videoUrlList = mongoDB.getVideoUrlList(visitorId,place);
        if(videoUrlList.size()>0){
            resultMap.put("videos",videoUrlList);
        }else {
            resultMap.put("videos",null);
        }
        return new JSONObject(resultMap).toJSONString();
    }

    //================ 体验区 ===================
    //添加游客Id
    @RequestMapping(value = "/anytec/visitor/add",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addVisitorId(@RequestParam("id")String visitorId,@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/visitor/add");
        logger.info("参数1：id="+visitorId);
        logger.info("参数2：location="+place);
        Map<String,Object> resultMap = new HashMap<>();

        if(expZoneService.addVisitorId(visitorId,place)){
            resultMap.put("code","success");
        }else {
            response.setStatus(400);
            resultMap.put("code","bad location");
        }
        return new JSONObject(resultMap).toJSONString();
    }

    //移除游客Id
    @RequestMapping(value = "/anytec/visitor/remove",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeVisitorId(@RequestParam("id")String visitorId,@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/visitor/remove");
        logger.info("参数1：id="+visitorId);
        logger.info("参数2：location="+place);
        Map<String,Object> resultMap = new HashMap<>();

        if(expZoneService.removeVisitorId(visitorId,place)){
            resultMap.put("code","success");
        }else {
            response.setStatus(400);
            resultMap.put("code","bad location");
        }
        return new JSONObject(resultMap).toJSONString();
    }

    //清空体验区游客Ids
    @RequestMapping(value = "/anytec/visitors/clear",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String clearVisitorIds(@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/visitors/clear");
        logger.info("参数1：location="+place);
        Map<String,Object> resultMap = new HashMap<>();

        if(expZoneService.clearVisitorIds(place)){
            resultMap.put("code","success");
        }else {
            response.setStatus(400);
            resultMap.put("code","bad location");
        }
        return new JSONObject(resultMap).toJSONString();
    }



    //开始录制体验区视频
    @RequestMapping(value = "/anytec/recording/start",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String startAreaVideo(@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/recording/start");
        logger.info("参数1：location="+place);
        Map<String,Object> resultMap = new HashMap<>();
        if(!expZoneService.locationCheck(place)){
            resultMap.put("code","bad location");
            response.setStatus(400);
            return new JSONObject(resultMap).toJSONString();
        }
        List<String> visitorIdList =expZoneService.getVisitorIdList(place);
        if(visitorIdList.size()==0){
            response.setStatus(400);
            resultMap.put("code","该区域无游客录入");
            return new JSONObject(resultMap).toJSONString();
        }
        if(expZoneService.saveAreaCamera(place)){
            resultMap.put("code","success");
        }else {
            resultMap.put("code",place+" 摄像机开启失败");
        }
        return new JSONObject(resultMap).toJSONString();
    }

    //================ 滑梯区 ===================
    @RequestMapping(value = "/anytec/waterslide/register",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String slidewayRegister(@RequestParam("id")String visitorId,HttpServletResponse response){
        logger.info("接口调用：/anytec/waterslide/register");
        logger.info("参数1：id="+visitorId);
        Map<String,Object> resultMap = new HashMap<>();
        slideService.setSlideId(visitorId);
        resultMap.put("ID",slideService.getSlideId());
        return new JSONObject(resultMap).toJSONString();
    }

    @RequestMapping(value = "/anytec/bgm/update",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateBGM(@RequestParam("bgm")MultipartFile audio, HttpServletResponse response){
        logger.info("接口调用：/anytec/bgm/update");
        Map<String,Object> reply = new HashMap<>();
        File file = new File(generalConfig.getBgm());
        byte[] bakBGM = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if(file.exists()){
                inputStream = new FileInputStream(file);
                bakBGM = Utils.streamToByte(inputStream);
            }
            audio.transferTo(file);
            FewMediaInfo info = ffmpegService.getMediaInfo(file);
            if(info == null || info.getAudioInfo() == null){
                reply.put("code","can't find audio");
                response.setStatus(400);
                if(bakBGM != null){
                    outputStream = new FileOutputStream(file);
                    outputStream.write(bakBGM);
                    outputStream.flush();
                }
                return new JSONObject(reply).toJSONString();
            }
            reply.put("code","success");
            return new JSONObject(reply).toJSONString();
        } catch (IOException e) {
            response.setStatus(500);
            logger.error("更新BGM失败");
            e.printStackTrace();
            return new JSONObject(reply).toJSONString();
        }finally {
            try {
                if(outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if(inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @RequestMapping(value = "/vod/list")
    @ResponseBody
    public String getList(@RequestHeader("Authorization")String keysecret){
        if(keysecret == null || !keysecret.equals(generalConfig.getAccessKeySecret())){
            return "{\"code\":\"bad Token\"}";
        }
        Map<String,String> params = new HashMap<>();
        params.put("Action","GetVideoList");
        try {
            return vodAPI.requestAPI(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{\"code\":\"error\"}";
    }

    @RequestMapping(value = "/vod/deleteAll")
    @ResponseBody
    public String deleteAll(@RequestHeader("Authorization")String keysecret){
        if(keysecret == null || !keysecret.equals(generalConfig.getAccessKeySecret())){
            return "{\"code\":\"bad Token\"}";
        }
        Map<String,String> params = new HashMap<>();
        params.put("Action","GetVideoList");
        try {
            String list =  vodAPI.requestAPI(params);
            JSONObject listJson = JSONObject.parseObject(list);
            JSONArray array = listJson.getJSONObject("VideoList").getJSONArray("Video");
            StringBuilder deletes = new StringBuilder();
            for (int i = 0; i <array.size() ; i++) {
                String id = array.getJSONObject(i).getString("VideoId");
                deletes.append(id);
                if(i != array.size()-1)
                    deletes.append(",");
            }
            Map<String,String> deleteParams = new HashMap<>();
            deleteParams.put("Action","DeleteVideo");
            deleteParams.put("VideoIds",deletes.toString());
            return vodAPI.requestAPI(deleteParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{\"code\":\"error\"}";
    }

    @RequestMapping(value = "/vod/upload")
    @ResponseBody
    public String upload(@RequestHeader("Authorization")String keysecret, @RequestParam("filename")String filename){
        if(keysecret == null || !keysecret.equals(generalConfig.getAccessKeySecret())){
            return "{\"code\":\"bad Token\"}";
        }
        UploadVideoResponse response = vodUpload.uploadVideo("test",filename,null,null,null,null,null,null);
        if(response.isSuccess()){
            return "{\"VideoId\":\""+response.getVideoId()+"\"}";
        }else {
            return "{\"code\":\"error\"}";
        }
    }

    @RequestMapping(value = "/vod/getVideoInfo")
    @ResponseBody
    public String getVideoInfo(@RequestHeader("Authorization")String keysecret, @RequestParam("videoId")String id){
        if(keysecret == null || !keysecret.equals(generalConfig.getAccessKeySecret())){
            return "{\"code\":\"bad Token\"}";
        }
        Map<String,String> params = new HashMap<>();
        params.put("Action","GetPlayInfo");
        params.put("VideoId",id);
        params.put("Formats","mp4");
        try {
            return vodAPI.requestAPI(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{\"code\":\"error\"}";
    }

    @RequestMapping(value = "/trigger")
    @ResponseBody
    public void trigger(){
        myApplicationRunner.parseIO("00000000000000000101");
    }
}
