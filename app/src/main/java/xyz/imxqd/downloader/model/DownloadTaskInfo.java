package xyz.imxqd.downloader.model;

import android.support.annotation.IntRange;

import com.orm.SugarRecord;

/**
 * Created by imxqd on 2016/7/4.
 * 下载任务的模型类
 */
public class DownloadTaskInfo extends SugarRecord {
    public static final int STATE_CREATE = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_PAUSE = 3;
    public static final int STATE_FINISH = 4;
    public static final int STATE_ERROR = 5;

    String filename = "", url, path;
    int totalSize, currentSize;
    int state = STATE_CREATE;
    boolean breakpoint = true;

    public DownloadTaskInfo(String filename, String url, String path
            , int totalSize, int currentSize,@IntRange(from = 1, to = 5) int state) {
        this.filename = filename;
        this.url = url;
        this.path = path;
        this.totalSize = totalSize;
        this.currentSize = currentSize;
        this.state = state;
    }

    public DownloadTaskInfo() {
    }

    public boolean isBreakpoint() {
        return breakpoint;
    }

    public void setBreakpoint(boolean breakpoint) {
        this.breakpoint = breakpoint;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
