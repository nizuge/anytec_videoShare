package cn.anytec.ffmpeg;

import java.io.File;

public interface FFMPEGService {
    FewMediaInfo getMediaInfo(File file);

    boolean convertVideo(File source, File output, boolean isCover);

    boolean preVideo(File source, File output, boolean isCover);

    boolean cutVideo(File source, File output, String start, String duration, boolean isCover);

    boolean deferVideo(File source, File output, Integer fps, boolean isCover);

    boolean concatVideos(File concatFile, File output, boolean isCover);

    boolean mergeAudio(File audio, File video, File output, boolean isCover);

}
