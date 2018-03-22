package cn.anytec.aliyun.vod;

import cn.anytec.config.GeneralConfig;
import com.aliyun.vod.upload.impl.UploadVideoImpl;
import com.aliyun.vod.upload.req.UploadURLStreamRequest;
import com.aliyun.vod.upload.req.UploadVideoRequest;
import com.aliyun.vod.upload.resp.UploadURLStreamResponse;
import com.aliyun.vod.upload.resp.UploadVideoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 以下Java示例代码演示了如何在服务端上传文件至视频点播。
 * 目前支持两种方式上传：
 * 1.上传本地文件，使用分片上传，并支持断点续传，最大支持48.8TB的单个文件。参见testUploadVideo函数。
 * 2.上传网络流，可指定文件URL进行上传，不支持断点续传，最大支持5GB的单个文件。参见testUploadURLStream函数。
 * 请替换示例中的必选参数，示例中的可选参数如果您不需要设置，请将其删除，以免设置无效参数值与您的预期不符。
 */
@Component
public class VodUpload {

    @Autowired
    GeneralConfig config;

    /**
     * 本地文件上传接口
     * @param title
     * @param fileName
     */
    public UploadVideoResponse uploadVideo( String title, String fileName,String callBackUrl,String coverUrl,Boolean showWaterMark,Integer cateId,String tags,String description) {
        UploadVideoRequest request = new UploadVideoRequest(config.getAccessKeyId(), config.getAccessKeySecret(), title, fileName);
        if(showWaterMark == null){
            request.setIsShowWaterMark(false);
        }else {
            request.setIsShowWaterMark(true);
        }
        //设置上传完成后的回调URL
        if(callBackUrl != null){
            request.setCallback(callBackUrl);
        }
        //视频分类ID(可选)
        if(cateId == null){
            request.setCateId(0);
        }else {
            request.setCateId(cateId);
        }
        //视频标签,多个用逗号分隔(可选)
        if(tags != null){
            request.setTags(tags);
        }
        //视频描述(可选)
        if(description != null){
            request.setDescription(description);
        }
        //封面图片(可选)
        if(coverUrl != null){
            request.setCoverURL(coverUrl);
        }
        request.setPartSize(10 * 1024 * 1024L);     //可指定分片上传时每个分片的大小，默认为10M字节
        request.setTaskNum(1);                      //可指定分片上传时的并发线程数，默认为1，(注：该配置会占用服务器CPU资源，需根据服务器情况指定）
        UploadVideoImpl uploader = new UploadVideoImpl();
        UploadVideoResponse response = uploader.uploadVideo(request);
        return response;
    }

    /**
     * 网络流上传接口
     * @param title
     * @param fileName
     * @param url
     */
    public UploadURLStreamResponse uploadURLStream(String title, String fileName, String url,String callBackUrl,String coverUrl,Boolean showWaterMark,Integer cateId,String tags,String description) {
        UploadURLStreamRequest request = new UploadURLStreamRequest(config.getAccessKeyId(), config.getAccessKeySecret(), title, fileName, url);
        //是否使用默认水印
        if(showWaterMark == null){
            request.setShowWaterMark(false);
        }else {
            request.setShowWaterMark(true);
        }
        //设置上传完成后的回调URL
        if(callBackUrl != null){
            request.setCallback(callBackUrl);
        }
        //视频分类ID(可选)
        if(cateId == null){
            request.setCateId(0);
        }else {
            request.setCateId(cateId);
        }
        //视频标签,多个用逗号分隔(可选)
        if(tags != null){
            request.setTags(tags);
        }
        //视频描述(可选)
        if(description != null){
            request.setDescription(description);
        }
        //封面图片(可选)
        if(coverUrl != null){
            request.setCoverURL(coverUrl);
        }
        UploadVideoImpl uploader = new UploadVideoImpl();
        UploadURLStreamResponse response = uploader.uploadURLStream(request);
        return response;
    }
}