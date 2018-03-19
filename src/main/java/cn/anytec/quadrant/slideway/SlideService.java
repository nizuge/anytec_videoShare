package cn.anytec.quadrant.slideway;

import cn.anytec.config.GeneralConfig;
import cn.anytec.quadrant.hcEntity.DeviceInfo;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import com.sun.jna.NativeLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class SlideService {

    private static final Logger logger = LoggerFactory.getLogger(SlideService.class);
    private static String slideId;
    private ThreadLocal threadLocal = new ThreadLocal();

    private DeviceInfo closeView;
    private DeviceInfo farView;

    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    GeneralConfig config;




    public void notifySlide(){
        threadLocal.set(slideId);
        try {
            if (!hcsdkHandler.loginCamera(farView)) {
                logger.error("远景摄像头注册失败");
                return;
            }
            if (!hcsdkHandler.loginCamera(closeView)) {
                logger.error("近景摄像头注册失败");
                return;
            }
            String contextPath = new StringBuilder(config.getVideoContext())
                    .append(File.separator).append(threadLocal.get()).toString();
            File visitorContext = new File(contextPath);
            if(!visitorContext.exists()){
                if(!visitorContext.mkdir()){
                    logger.error("创建游客文件夹失败");
                    return;
                }
            }
            logger.info("开启远景摄像头预览:"+farView.getDeviceIp());
            SlideDataCallBack farCallBack = new SlideDataCallBack(new File(visitorContext,"far.tmp"));
            NativeLong lRealPlayHandle_far = hcsdkHandler.preView(farView,farCallBack);
            Thread.sleep(config.getDelay());
            logger.info("开启近景摄像头预览："+closeView.getDeviceIp());
            SlideDataCallBack closeCallBack = new SlideDataCallBack(new File(visitorContext,"close.tmp"));
            NativeLong lRealPlayHandle_close = hcsdkHandler.preView(closeView,closeCallBack);
            Thread.sleep(config.getDuration0());
            hcsdkHandler.stopPreView(lRealPlayHandle_close);
            logger.info("关闭近景摄像头预览");
            closeCallBack.close();
            Thread.sleep(config.getDuration1() - config.getDelay() - config.getDuration0());
            hcsdkHandler.stopPreView(lRealPlayHandle_far);
            logger.info("关闭远景摄像头预览");
            farCallBack.close();
            logger.info("视频流写入完毕："+threadLocal.get());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void setFarView(DeviceInfo farView) {
        this.farView = farView;
    }
    public void setCloseView(DeviceInfo closeView) {
        this.closeView = closeView;
    }
    public void setSlideId(String slideId) {SlideService.slideId = slideId;}
    public String getSlideId() {return slideId;}
}
