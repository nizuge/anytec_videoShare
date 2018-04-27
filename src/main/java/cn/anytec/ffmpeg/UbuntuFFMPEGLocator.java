package cn.anytec.ffmpeg;

import it.sauronsoftware.jave.FFMPEGLocator;

/**
 * 重新指定ffmpeg程序位置
 */
public class UbuntuFFMPEGLocator extends FFMPEGLocator {
    @Override
    protected String getFFMPEGExecutablePath() {
        return "/usr/bin/ffmpeg";
    }
}
