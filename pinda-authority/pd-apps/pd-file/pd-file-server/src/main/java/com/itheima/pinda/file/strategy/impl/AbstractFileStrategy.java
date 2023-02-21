package com.itheima.pinda.file.strategy.impl;

import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.enumeration.IconType;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.FileStrategy;
import com.itheima.pinda.file.utils.FileDataTypeUtil;
import com.itheima.pinda.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.apache.bcel.classfile.CodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抽象文件策略处理类
 */
@Slf4j
public abstract class AbstractFileStrategy implements FileStrategy {
    private static final String FILE_SPLIT = ".";

    @Autowired
    protected FileServerProperties fileProperties;

    protected FileServerProperties.Properties properties;
    /**
     * 文件上传
     * @param multipartFile
     * @return
     */
    @Override
    public File upload(MultipartFile multipartFile) {
        try {
            //获得上传文件的原始文件名称
            String originalFilename = multipartFile.getOriginalFilename();
            if(!originalFilename.contains(FILE_SPLIT)){
                //文件名称中没有.，这是非法的，直接抛出异常
                throw BizException.wrap(ExceptionCode.BASE_VALID_PARAM.build("上传文件名称缺少后缀"));
            }

            //封装一个File对象，在完成文件上传后需要将上传的文件信息保存到数据库
            File file = File.builder()
                        .isDelete(false) //文件是否被删除
                        .size(multipartFile.getSize()) //文件大小
                        .contextType(multipartFile.getContentType()) //文件类型
                        .dataType(FileDataTypeUtil.getDataType(multipartFile.getContentType())) //数据类型：
                        .submittedFileName(multipartFile.getOriginalFilename()) //原始文件名称
                        .ext(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))
                        .build();

            //设置文件的图标
            file.setIcon(IconType.getIcon(file.getExt()).getIcon());

            LocalDateTime now = LocalDateTime.now();

            //设置文件创建时间
            file.setCreateMonth(DateUtils.formatAsYearMonthEn(now));
            file.setCreateWeek(DateUtils.formatAsYearWeekEn(now));
            file.setCreateDay(DateUtils.formatAsDateEn(now));

            uploadFile(file,multipartFile);
            return file;
        }catch (Exception e){
            log.error("e = {}",e);
            throw BizException.wrap(ExceptionCode.BASE_VALID_PARAM.build("文件上传失败"));
        }
    }

    /**
     * 文件上传抽象方法，需要由当前类的子类来实现
     * @param file
     * @param multipartFile
     * @return
     */
    public abstract void uploadFile(File file,MultipartFile multipartFile) throws Exception;

    /**
     * 文件删除
     * @param list
     * @return
     */
    @Override
    public boolean delete(List<FileDeleteDO> list) {
        if(list == null || list.isEmpty()){
            return true;
        }

        boolean flag = false;//删除操作是否成功的标识位
        for (FileDeleteDO fileDeleteDO : list) {
            try {
                delete(fileDeleteDO);
                flag = true;
            }catch (Exception e){
                log.error("e = {}",e);
            }
        }

        return flag;
    }

    /**
     * 文件删除抽象方法，需要当前类的子类来实现
     * @param fileDeleteDO
     */
    public abstract void delete(FileDeleteDO fileDeleteDO);

    /**
     * 获取下载地址前缀
     */
    protected String getUriPrefix() {
        if (StringUtils.isNotEmpty(properties.getUriPrefix())) {
            return properties.getUriPrefix();
        } else {
            return properties.getEndpoint();
        }
    }

}
