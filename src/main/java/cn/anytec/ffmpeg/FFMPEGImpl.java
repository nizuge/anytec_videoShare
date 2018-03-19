package cn.anytec.ffmpeg;

import cn.anytec.util.RuntimeLocal;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.MultimediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FFMPEGImpl implements FFMPEGService {

    private static final Logger logger = LoggerFactory.getLogger(FFMPEGImpl.class);
    private static final Encoder encoder = new Encoder(new UbuntuFFMPEGLocator());

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
        logger.debug(new RuntimeLocal().execute(new String[]{
                "ffmpeg","-i",source.getAbsolutePath(),
                "-strict","-2",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            source.delete();
            return true;
        }
        return false;
    }

    @Override
    public boolean preVideo(File source, File output,boolean isCover) {
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        logger.debug(new RuntimeLocal().execute(new String[]{
                "ffmpeg", "-i",source.getAbsolutePath(),
                "-strict","-2","-qscale","0","-intra",
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
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        logger.debug(new RuntimeLocal().execute(new String[]{
                "ffmpeg", "-ss",start,"-t",duration,
                "-i",source.getAbsolutePath(),
                "-vcodec","copy","-acodec","copy","-strict","-2",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean deferVideo(File source, File output, Integer fps, boolean isCover) {
        if(getMediaInfo(source) == null){
            logger.error("无效输入："+source);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        logger.debug(new RuntimeLocal().execute(new String[]{
                "ffmpeg","-r",fps.toString(),
                "-i",source.getAbsolutePath(),
                "-strict","-2",
                "-y",output.getAbsolutePath()
        }));
        if(output.exists()){
            return true;
        }
        return false;
    }

    @Override
    public boolean concatVideos(File concatFile, File output, boolean isCover) {
        if(getMediaInfo(concatFile) == null){
            logger.error("无效输入："+concatFile);
            return false;
        }
        if(!isCover && output.exists()){
            logger.warn("输出文件已存在");
            return false;
        }
        logger.debug(new RuntimeLocal().execute(new String[]{
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
        if(audioDuration >= videoDuration){
            logger.debug(new RuntimeLocal().execute(new String[]{
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
            logger.debug(new RuntimeLocal().execute(new String[]{
                    "ffmpeg","-i",concatAudio.toString(),
                    "-acodec","copy",
                    "-y",tmpAudio.getAbsolutePath()
            }));
            if(tmpAudio.exists()){
                logger.debug(new RuntimeLocal().execute(new String[]{
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
}
