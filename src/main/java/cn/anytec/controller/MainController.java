package cn.anytec.controller;

import cn.anytec.config.GeneralConfig;

import cn.anytec.ffmpeg.FFMPEGService;
import cn.anytec.ffmpeg.FewMediaInfo;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.quadrant.expZone.ExpZoneService;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import cn.anytec.quadrant.slideway.SlideService;
import cn.anytec.util.Utils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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

    //根据Id和地点获取视频地址
    @RequestMapping(value = "/anytec/videos",method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getVideoList(@RequestParam("id")String visitorId,@RequestParam("location")String place,HttpServletResponse response){
        logger.info("接口调用：/anytec/videos");
        logger.info("参数1：id="+visitorId);
        logger.info("参数2：location="+place);
        Map<String,Object> resultMap = new HashMap<>();
        if(!expZoneService.locationCheck(place)){
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
        }
        return new JSONObject(resultMap).toJSONString();
    }

    //================ 滑梯区 ===================
    @RequestMapping(value = "/anytec/waterslide/register",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String slidewayRegister(@RequestParam("id")String visitorId,HttpServletResponse response){
        logger.info("接口调用：/anytec/waterslide/register");
        logger.info("参数1：id="+visitorId);
        Map<String,String> resultMap = new HashMap<>();
        slideService.setSlideId(visitorId);
        resultMap.put("ID",slideService.getSlideId());
        return new JSONObject(resultMap).toJSONString();
    }

    @RequestMapping(value = "/anytec/bgm/update",method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateBGM(@RequestParam("bgm")MultipartFile audio, HttpServletResponse response){
        logger.info("接口调用：/anytec/bgm/update");
        Map<String,String> reply = new HashMap<>();
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


}
