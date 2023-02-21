package com.itheima.pinda.file.manager;

import com.itheima.pinda.file.dto.chunk.FileUploadDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 封装分片操作的工具类，主要用户创建分片临时文件、分片存放的目录
 */
@Component
@Slf4j
public class WebUploader {
    /**
     * 为分片上传创建对应的保存位置，同时还可以创建临时文件.tmp
     * @param fileUploadDTO
     * @param path
     * @return
     */
    public java.io.File getReadySpace(FileUploadDTO fileUploadDTO,String path){// path ===> D:\\uploadFiles\\2020\\05
        boolean b = createFileFolder(path,false);
        if(!b){
            return null;
        }

        //将分片文件保存到文件名对应的md5构成的目录中
        String fileFolder = fileUploadDTO.getName();

        if(fileFolder == null){
            return null;
        }

        path += "/" + fileFolder;//path ===> D:\\uploadFiles\\2020\\05\\wwwww

        //创建临时文件和存放分片的目录
        b = createFileFolder(path, true);
        if(!b){
            return null;
        }

        //构造需要上传的分片文件对应的路径 D:\\uploadFiles\\2020\\05\\wwwww\\3
        return new java.io.File(path,String.valueOf(fileUploadDTO.getChunk()));
    }

    /**
     * 具体执行创建分片所在的目录和临时文件
     * @param file
     * @param hasTmp
     * @return
     */
    private boolean createFileFolder(String file,boolean hasTmp){ // D:\\uploadFiles
        java.io.File tmpFile = new java.io.File(file);
        if(!tmpFile.exists()){
            //不存在，直接创建
            try {
                tmpFile.mkdirs();//创建目录
            }catch (Exception ex){
                log.error("创建分片所在目录失败",ex);
                return false;
            }
        }
        if(hasTmp){
            //需要创建临时文件
            tmpFile = new java.io.File(file + ".tmp"); // D:\\uploadFiles\\2020\\05\\abc.tmp
            if(tmpFile.exists()){
                //临时文件已经存在，修改临时文件的最后更新时间为当前系统时间
                return tmpFile.setLastModified(System.currentTimeMillis());
            }else{
                //临时文件不存在，需要创建
                try {
                    tmpFile.createNewFile();//创建文件
                }catch (Exception ex){
                    log.error("创建分片对应的临时文件失败",ex);
                    return false;
                }
            }
        }
        return true;
    }
}
