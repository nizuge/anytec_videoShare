package cn.anytec.ffmpeg;

import cn.anytec.config.GeneralConfig;
import cn.anytec.util.RuntimeLocal;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.MultimediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FFMPEGImpl implements FFMPEGService {

    private static final Logger logger = LoggerFactory.getLogger(FFMPEGImpl.class);
    private static final Encoder encoder = new Encoder(new UbuntuFFMPEGLocator());
    private static RuntimeLocal runtimeLocal = new RuntimeLocal();

    @Autowired
    private GeneralConfig config;

    @Override
    public FewMediaInfo getMediaInfo(File file) {
        if(!file.exists()){
            logger.error("要解析的媒体文件不存在！");
            return null;
        }
        FewMediaInfo mediaInfo = null;
        try {
            MultimediaInfo multimediaInfo = encoder.getInfo(file);
            mediaInfo = new FewMediaInfo();
            mediaInfo.setFormat(multimediaInfo.getFormat());
            mediaInfo.setDuration(multimediaInfo.getDuration());
            mediaInfo.setVideoInfo(multimediaInfo.getVideo());
            mediaInfo.setAudioInfo(multimediaInfo.getAudio());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return mediaInfo;
    }

    @Override
    public boolean convertVideo(File source, File output ,boolean isCover) {
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg","-i",source.getAbsolutePath(),
                "-c:v","libx264","-c:a","aac","-strict","experimental","-b:a","98k",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            source.delete();
            return true;
        }
        return false;
    }

    @Override
    public boolean cutVideo(File source, File output, String start, String duration,boolean isCover) {
        FewMediaInfo info = getMediaInfo(source);
        if(info == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg", "-ss",start,"-t",duration,
                "-i",source.getAbsolutePath(),
                "-c:v","libx264","-c:a","aac","-strict","experimental","-b:a","98k",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean cutEnd(File source, File output,double endDuration ,boolean isCover) {
        FewMediaInfo info = getMediaInfo(source);
        if(info == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        logger.info(info.getDuration()+"--"+endDuration);
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        double t = info.getDuration()-endDuration;
        if(t<0){
            t = 8.0;
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg", "-ss","0","-t",Double.valueOf(t).toString(),
                "-i",source.getAbsolutePath(),
                "-c:v","libx264","-c:a","aac","-strict","experimental","-b:a","98k",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean changeFps(File source, File output, Integer fps, boolean isCover) {
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg","-r",fps.toString(),
                "-i",source.getAbsolutePath(),
                "-strict","-2",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            //source.delete();
            return true;
        }
        return false;
    }

    @Override
    public boolean deferVideo(File source, File output, Integer fps,double pts, boolean isCover) {
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(pts == 0.0){
            logger.info("Do not use PTS");
            logger.debug(runtimeLocal.execute(new String[]{
                    "ffmpeg","-r",fps.toString(),
                    "-i",source.getAbsolutePath(),
                    "-strict","-2",
                    "-y",output.getAbsolutePath()
            }));
        }else {
            logger.debug(runtimeLocal.execute(new String[]{
                    "ffmpeg","-r",fps.toString(),
                    "-i",source.getAbsolutePath(),
                    "-filter:v","setpts="+pts+"*PTS",
                    "-strict","-2",
                    "-y",output.getAbsolutePath()
            }));
        }

        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean concatMedia(File concatFile, File output, boolean isCover) {
        if(getMediaInfo(concatFile) == null){
            logger.error("无效输入："+concatFile);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg", "-f","concat","-safe","0",
                "-i",concatFile.getAbsolutePath(),
                "-c","copy",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean mergeAudio(File audio, File video, File output,boolean isCover) {
        FewMediaInfo audioInfo = getMediaInfo(audio);
        if(audioInfo == null){
            logger.error("无效的音频文件输入");
            return false;
        }
        FewMediaInfo videoInfo = getMediaInfo(video);
        if(videoInfo == null){
            logger.error("无效的视频文件输入");
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        Float videoDuration = videoInfo.getDuration();
        float audioDuration = audioInfo.getDuration();
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(audioDuration >= videoDuration){
            logger.debug(runtimeLocal.execute(new String[]{
                    "ffmpeg","-i",video.getAbsolutePath(),
                    "-i",audio.getAbsolutePath(),
                    "-filter_complex","[1:0]apad","-shortest","-strict","-2",
                    "-y",output.getAbsolutePath()
            }));
            if(output.exists())
                return true;
        }else{
            File tmpAudio = new File(video.getParentFile(),"tmp."+audio.getName().split("\\.")[1]);
            int audio_replayTimes = ((int)(videoDuration/audioDuration))+1;
            StringBuilder concatAudio = new StringBuilder("concat:").append(audio.getAbsolutePath());
            for (int i = 0; i < audio_replayTimes; i++) {
                concatAudio.append("|").append(audio.getAbsolutePath());
            }
            logger.debug(runtimeLocal.execute(new String[]{
                    "ffmpeg","-i",concatAudio.toString(),
                    "-acodec","copy",
                    "-y",tmpAudio.getAbsolutePath()
            }));
            while (runtimeLocal.isAlive()){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(tmpAudio.exists()){
                logger.debug(runtimeLocal.execute(new String[]{
                        "ffmpeg","-i",video.getAbsolutePath(),
                        "-i",tmpAudio.getAbsolutePath(),
                        "-filter_complex","[1:0]apad","-shortest","-strict","-2",
                        "-y",output.getAbsolutePath()
                }));
                if(output.exists()){
                    tmpAudio.delete();
                    return true;
                }

            }
            logger.error("短音频处理异常");
        }
        return false;
    }

    @Override
    public boolean separateAudio(File video, File music, boolean isCover) {
        if(getMediaInfo(video) == null){
            logger.error("无效输入："+video);
            return false;
        }
        if(!isCover && music.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg","-i",video.getAbsolutePath(),
                "-vn",music.getAbsolutePath()
        }));
        if(music.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean separateVideo(File video, File videoAN, boolean isCover) {
        if(getMediaInfo(video) == null){
            logger.error("无效输入："+video);
            return false;
        }
        if(!isCover && videoAN.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        while (runtimeLocal.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug(runtimeLocal.execute(new String[]{
                "ffmpeg","-i",video.getAbsolutePath(),
                "-an",videoAN.getAbsolutePath()
        }));
        if(videoAN.exists()){
            return true;
        }
        return false;
    }

}
