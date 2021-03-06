package com.wenshanhu.endecry.helper;

import android.os.Handler;
import android.os.Looper;

import com.wenshanhu.endecry.bean.SteamBean;
import com.wenshanhu.endecry.receiver.USBReceiver;
import com.yhd.endecry.EnDecryHelper;
import com.yhd.utils.EnDecryUtil;

import java.io.File;

/**
 * 类作用描述
 * Created by haide.yin(haide.yin@tcl.com) on 2019/11/12 14:50.
 */
public class SteamHelper {

    private static SteamHelper singleton;
    private SteamBean steamBean;
    private Handler mainHandler;//主线程

    /**
     * 单例
     */
    public static synchronized SteamHelper get(){
        if(singleton == null){
            singleton = new SteamHelper();
        }
        return singleton;
    }

    private SteamHelper(){
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 更新资源路径机构
     */
    public void init(InitCallback initCallback){
        new Thread(() -> {
            long beginTime = System.currentTimeMillis();
            steamBean = new SteamBean(USBReceiver.USB_PATH + File.separator + USBReceiver.STEAM_PATH);
            long endTime = System.currentTimeMillis();
            if(initCallback != null){
                mainHandler.post(() -> initCallback.onFinish(endTime-beginTime));
            }
        }).start();
    }

    /**
     * 获取资源路径机构
     */
    public SteamBean getSteamBean() {
        if(steamBean == null){
            steamBean = new SteamBean(USBReceiver.USB_PATH + File.separator + USBReceiver.STEAM_PATH);
        }
        return steamBean;
    }

    /**
     * 开始加密
     */
    public void beginEncry(CryCallback cryCallback){
        new Thread(() -> {
            String sourcePath = steamBean.getEnDeCryBean().getSourcePath();
            String encryPath = steamBean.getEnDeCryBean().getEncryPath();
            File sourceFileFolder = new File(sourcePath);
            File encryFileFolder = new File(encryPath);
            if(!sourceFileFolder.exists() || !sourceFileFolder.isDirectory()){
                onError(cryCallback,"endecry/source文件夹不存在");
                return;
            }
            if(!encryFileFolder.exists() || !encryFileFolder.isDirectory()){
                onError(cryCallback,"endecry/encry文件夹不存在");
                return;
            }
            String[] sourceFiles = sourceFileFolder.list();
            if(sourceFiles == null || sourceFiles.length == 0){
                onError(cryCallback,"endecry/source文件夹没有文件");
                return;
            }
            for(String fileName : sourceFiles){
                String sourceFilePath = sourcePath + File.separator + fileName;
                String encryFilePath = encryPath + File.separator + fileName;
                //处理结尾
                if(encryFilePath.contains(".")){
                    String[] nameArray = encryFilePath.split("\\.");
                    if(nameArray.length > 0){
                        String suffix = EnDecryUtil.SUFFIX_V;
                        if(fileName.endsWith(EnDecryUtil.MP4)){
                            suffix = EnDecryUtil.SUFFIX_V;
                        }else if(fileName.endsWith(EnDecryUtil.MP3)){
                            suffix = EnDecryUtil.SUFFIX_M;
                        }else if(fileName.endsWith(EnDecryUtil.PNG)){
                            suffix = EnDecryUtil.SUFFIX_P;
                        }else if(fileName.endsWith(EnDecryUtil.JPG)){
                            suffix = EnDecryUtil.SUFFIX_J;
                        }else if(fileName.endsWith(EnDecryUtil.TXT)){
                            suffix = EnDecryUtil.SUFFIX_T;
                        }
                        //统一更换加密后的后缀
                        encryFilePath = encryFilePath.replace(nameArray[nameArray.length - 1],suffix);
                    }
                }
                if(new File(sourceFilePath).exists()){
                    EnDecryUtil.writeToLocal(EnDecryUtil.deEncrypt(sourceFilePath),encryFilePath);
                    onProgress(cryCallback,"正在加密:"+encryFilePath);
                }else{
                    onError(cryCallback,"文件"+sourceFilePath+"不存在");
                }
            }
            onFinish(cryCallback,"加密完成");
        }).start();
    }

    /**
     * 开始解密
     */
    public void beginDecry(CryCallback cryCallback){
        new Thread(() -> {
            String sourcePath = steamBean.getEnDeCryBean().getSourcePath();
            String decryPath = steamBean.getEnDeCryBean().getDecryPath();
            File sourceFileFolder = new File(sourcePath);
            File decryFileFolder = new File(decryPath);
            if(!sourceFileFolder.exists() || !sourceFileFolder.isDirectory()){
                onError(cryCallback,"endecry/source文件夹不存在");
                return;
            }
            if(!decryFileFolder.exists() || !decryFileFolder.isDirectory()){
                onError(cryCallback,"endecry/decry文件夹不存在");
                return;
            }
            String[] sourceFiles = sourceFileFolder.list();
            if(sourceFiles == null || sourceFiles.length == 0){
                onError(cryCallback,"endecry/source文件夹没有文件");
                return;
            }
            for(String fileName : sourceFiles){
                String sourceFilePath = sourcePath + File.separator + fileName;
                String decryFilePath = decryPath + File.separator + fileName;
                //处理结尾
                if(decryFilePath.contains(".")){
                    String[] nameArray = decryFilePath.split("\\.");
                    if(nameArray.length > 0){
                        String suffix = EnDecryUtil.MP4;
                        if(fileName.endsWith(EnDecryUtil.SUFFIX_V)){
                            suffix = EnDecryUtil.MP4;
                        }else if(fileName.endsWith(EnDecryUtil.SUFFIX_M)){
                            suffix = EnDecryUtil.MP3;
                        }else if(fileName.endsWith(EnDecryUtil.SUFFIX_P)){
                            suffix = EnDecryUtil.PNG;
                        }else if(fileName.endsWith(EnDecryUtil.SUFFIX_T)){
                            suffix = EnDecryUtil.TXT;
                        }else if(fileName.endsWith(EnDecryUtil.SUFFIX_J)){
                            suffix = EnDecryUtil.JPG;
                        }
                        //统一更换加密后的后缀
                        decryFilePath = decryFilePath.replace(nameArray[nameArray.length - 1],suffix);
                    }
                }
                if(new File(sourceFilePath).exists()){
                    EnDecryUtil.writeToLocal(EnDecryUtil.deEncrypt(sourceFilePath),decryFilePath);
                    onProgress(cryCallback,"正在解密:"+decryFilePath);
                }else{
                    onError(cryCallback,"文件"+sourceFilePath+"不存在");
                }
            }
            onFinish(cryCallback,"解密完成");
        }).start();
    }

    /**
     * 加解密回调统一处理
     */
    private void onProgress(CryCallback cryCallback,String msg){
        if(cryCallback != null){
            mainHandler.post(() -> cryCallback.onProgress(msg));
        }
    }

    /**
     * 加解密回调统一处理
     */
    private void onError(CryCallback cryCallback,String msg){
        if(cryCallback != null){
            mainHandler.post(() -> cryCallback.onError(msg));
        }
    }

    /**
     * 加解密回调统一处理
     */
    private void onFinish(CryCallback cryCallback,String msg){
        if(cryCallback != null){
            mainHandler.post(() -> cryCallback.onFinish(msg));
        }
    }

    /**
     * 初始化的回调
     */
    public interface InitCallback{
        void onFinish(long msec);
    }

    /**
     * 加解密的回调
     */
    public interface CryCallback{
        void onProgress(String msg);
        void onError(String msg);
        void onFinish(String msg);
    }
}
