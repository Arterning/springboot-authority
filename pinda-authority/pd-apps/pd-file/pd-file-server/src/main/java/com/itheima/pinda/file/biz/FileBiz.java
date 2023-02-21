package com.itheima.pinda.file.biz;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.file.domain.FileDO;
import com.itheima.pinda.file.enumeration.DataType;
import com.itheima.pinda.file.utils.ZipUtils;
import com.itheima.pinda.utils.NumberHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提供文件下载的公共方法
 */
@Component
@Slf4j
public class FileBiz {

    //Java编程思想.doc  2 ===>Java编程思想(2).doc
    /**
     * 构建下载的文件名称
     * @param fileName
     * @param order
     * @return
     */
    private static String buildNewFileName(String fileName,Integer order){
        return StrUtil.strBuilder(fileName).insert(fileName.lastIndexOf("."),"(" + order + ")").toString();
    }

    /**
     * 文件下载方法
     * @param fileDOList
     * @param request
     * @param response
     */
    public void down(List<FileDO> fileDOList, HttpServletRequest request, HttpServletResponse response) throws Exception{
        int fileSize = fileDOList.stream().filter(//将fileDOList进行过滤
                    (file) -> file != null &&
                            !file.getDataType().eq(DataType.DIR) &&
                            StringUtils.isNotEmpty(file.getUrl())).mapToInt(//将文件大小转为int类型
                                    (file) -> NumberHelper.intValueOf0(file.getSize()
        )).sum();//计算要下载的文件总大小

        //确定要下载的文件名称
        String downLoadFileName = fileDOList.get(0).getSubmittedFileName();
        if(fileDOList.size() > 1){ //Java编程思想.doc ===> Java编程思想等.zip
            //要下载多个文件，生成一个zip压缩文件名称
            downLoadFileName = StringUtils.substring(downLoadFileName,0,StringUtils.lastIndexOf(downLoadFileName,".")) + "等.zip";
        }

        //fileDOList ===> Map<String,String>
        Map<String, String> fileMap = new LinkedHashMap<>(fileDOList.size());
        //处理下载文件名称同名情况
        Map<String,Integer> duplicateFile = new HashMap<>(fileDOList.size());

        fileDOList.stream().filter((file) -> file != null &&
                !file.getDataType().eq(DataType.DIR) &&
                StringUtils.isNotEmpty(file.getUrl())).forEach((file) -> {
                    //原始文件名称
            String submittedFileName = file.getSubmittedFileName();
            if(fileMap.containsKey(submittedFileName)){
                if(duplicateFile.containsKey(submittedFileName)){
                    //将重复的文件名次数加1处理
                    duplicateFile.put(submittedFileName,duplicateFile.get(submittedFileName) + 1);
                }else{
                    duplicateFile.put(submittedFileName,1);
                }
                submittedFileName = buildNewFileName(submittedFileName,duplicateFile.get(submittedFileName));
            }
            fileMap.put(submittedFileName,file.getUrl());
        });

        ZipUtils.zipFilesByInputStream(fileMap,Long.valueOf(fileSize),downLoadFileName,request,response);
    }
}
