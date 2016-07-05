package xyz.imxqd.downloader.utils;

import android.os.Environment;

import java.io.File;
import java.net.URL;
import java.util.Locale;

/**
 * Created by imxqd on 2016/7/4.
 * 获取文件名的工具类
 */
public class FileUtil {
    private static final File DIR = Environment.
            getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    /**
     * 根据URL的路径来获取文件名
     * @param url 用来获取文件名的URL
     * @return 文件名
     */
    public static String getFileName(URL url) {
        String str = url.toString();
        int pos = str.lastIndexOf("/");
        if(pos == -1) {
            return null;
        } else {
            return getRealFileName(str.substring(pos + 1));
        }
    }

    /**
     * 根据http header 中的contentDisposition获取文件名
     * @param contentDisposition http header 中的contentDisposition
     * @return 文件名
     */
    public static String getFileName(String contentDisposition) {
        if(contentDisposition == null) {
            return null;
        }
        int pos = contentDisposition.indexOf("filename=");
        String tmp = contentDisposition.substring(pos + 9);
        tmp = tmp.replace("\"", "");
        if(pos != -1) {
            return getRealFileName(tmp);
        }
        return null;
    }

    /**
     * 根据contentType去创建文件名
     * @param contentType http header中的contentType
     * @return 文件名
     */
    public static String createFileName(String contentType) {
        String suffix = contentType.substring(contentType.indexOf("/") + 1);
        String name = "no_name";
        File file = new File(DIR, name + "." + suffix);
        if (file.exists()) {
            for (int i = 0; i < 10000; i++) {
                file = new File(DIR, name + "(" + i + ")." + suffix);
                if(!file.exists()) {
                    break;
                }
            }
        }
        return file.getName();
    }

    /**
     * 给定一个目标文件名,根据文件是否已经存在来返回可用的文件名
     * @param filename 目标文件名
     * @return 最终文件名
     */
    private static String getRealFileName(String filename) {
        int pos = filename.lastIndexOf(".");
        String suffix = filename.substring(pos + 1);
        String name = filename.substring(0, pos);
        File file = new File(DIR, name + "." + suffix);
        if (file.exists()) {
            for (int i = 1; i < 10000; i++) {
                file = new File(DIR, name + "(" + i + ")." + suffix);
                if(!file.exists()) {
                    break;
                }
            }
        }
        return file.getName();
    }

    /**
     * 给定指定的字节数,返回合适的包含单位的文件大小字符串
     * @param size 字节数
     * @return 合适的包含单位的文件大小字符串
     */
    public static String parseFileSize(int size) {
        if(size < 1024) {
            return size + "B";
        } else if(size < 1024 * 1024) {
            return String.format(Locale.CHINESE,"%.2f",size / 1024.0) + "KB";
        } else {
            return String.format(Locale.CHINESE,"%.2f",size / 1024.0 / 1024.0)+ "MB";
        }
    }
}
