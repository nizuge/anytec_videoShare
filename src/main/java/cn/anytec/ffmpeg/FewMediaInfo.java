package cn.anytec.ffmpeg;

import it.sauronsoftware.jave.AudioInfo;
import it.sauronsoftware.jave.VideoInfo;

public class FewMediaInfo {
    private String format;
    private float duration;
    private AudioInfo audioInfo;
    private VideoInfo videoInfo;

    protected FewMediaInfo(){}

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration =  duration/1000f;
    }

    public AudioInfo getAudioInfo() {
        return audioInfo;
    }

    public void setAudioInfo(AudioInfo audioInfo) {
        this.audioInfo = audioInfo;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }
}
