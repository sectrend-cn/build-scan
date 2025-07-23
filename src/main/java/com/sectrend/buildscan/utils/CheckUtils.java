package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.model.NewScanInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 业务校验工具类
 */
public class CheckUtils {

    private static final Logger logger = LoggerFactory.getLogger(CheckUtils.class);

    /**
     * 校验是否登录
     * @param newScanInfo
     */
    public static void checkLogin(NewScanInfo newScanInfo){
        // 如果没传服务地址, 直接返回错误
        if (StringUtils.isEmpty(newScanInfo.getServerUrl())){
            logger.warn("缺少必传参数:serverUrl");
            System.exit(5);
        }
        // 如果没传token
        if (StringUtils.isEmpty(newScanInfo.getToken())){
            if (StringUtils.isEmpty(newScanInfo.getUsername()) || StringUtils.isEmpty(newScanInfo.getPassword())) {
                logger.warn("缺少必传参数:username,password");
                System.exit(5);
            }
        }
    }


    /**
     * 校验扫描类型
     * @param newScanInfo
     */
    public static void checkScanType(NewScanInfo newScanInfo){
        if (StringUtils.isBlank(newScanInfo.getScanType())) {
            logger.error("扫描类型为空");
            System.exit(3);
        }
    }


    /**
     * 校验扫描路径
     * @param newScanInfo
     */
    public static void checkLocationAndType(NewScanInfo newScanInfo){
        if (StringUtils.isBlank(newScanInfo.getTaskDir()) || !new File(newScanInfo.getTaskDir()).exists()  || !new File(newScanInfo.getTaskDir()).isDirectory()) {
            logger.info("文件目录不存在");
            System.exit(10);
        }

    }

    /**
     * 校验指纹文件路径
     * @param newScanInfo
     */
    public static void checkFromPath(NewScanInfo newScanInfo){
        if (StringUtils.isBlank(newScanInfo.getFromPath()) || (!new File(newScanInfo.getFromPath()).exists() && !new File(newScanInfo.getFromPath()).isDirectory())) {
            logger.error("文件目录不存在");
            System.exit(10);
        }
    }

    /**
     * 校验docker上传路径
     * @param newScanInfo
     */
    public static void checkTaskFileDir(NewScanInfo newScanInfo){
        if (StringUtils.isBlank(newScanInfo.getTaskFileDir())  || !new File(newScanInfo.getTaskFileDir()).exists()) {
            logger.error(newScanInfo.getTaskFileDir() + ", {}上传文件不存在", newScanInfo.getScanType());
            System.exit(10);
        }
    }

    /**
     * 校验依赖文件路径
     * @param txtDir
     */
    public static boolean checkTxtDir(String txtDir){
        if (StringUtils.isBlank(txtDir)) {
            logger.error("依赖文件路径为空");
            return false;
        }

        if(!new File(txtDir).exists()){
            logger.error(txtDir + ", 依赖文件不存在");
            return false;
        }
        return true;
    }



}
