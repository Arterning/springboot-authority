package com.itheima.pinda.file.storage;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileChunkStrategy;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import com.itheima.pinda.file.utils.FileDataTypeUtil;
import com.itheima.pinda.utils.DateUtils;
import com.itheima.pinda.utils.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地上传策略配置类
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type",havingValue = "LOCAL")
public class LocalAutoConfigure {
    /**
     * 本地文件策略处理类
     */
    @Service
    public class LocalServiceImpl extends AbstractFileStrategy{
        private void buildClient(){
            properties = fileProperties.getLocal();
        }

        /**
         * 文件上传
         * @param file
         * @param multipartFile
         */
        @Override
        public void uploadFile(File file, MultipartFile multipartFile) throws Exception{
            buildClient();
            String endpoint = properties.getEndpoint();
            String bucketName = properties.getBucketName();
            String uriPrefix = properties.getUriPrefix();

            //使用UUID为文件生成新文件名
            String fileName = UUID.randomUUID().toString() + StrPool.DOT + file.getExt();

            //  D:\\uploadFiles\\oss-file-service\\2020\\05\\xxx.doc
            //日期目录
            String relativePath = Paths.get(LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_MONTH_FORMAT_SLASH))).toString();

            //上传文件存储的绝对目录 例如：D:\\uploadFiles\\oss-file-service\\2020\\05
            String absolutePath = Paths.get(endpoint, bucketName, relativePath).toString();

            //目标输出文件D:\\uploadFiles\\oss-file-service\\2020\\05\\xxx.doc
            java.io.File outFile = new java.io.File(Paths.get(absolutePath,fileName).toString());

            //向目标文件写入数据
            FileUtils.writeByteArrayToFile(outFile,multipartFile.getBytes());

            //文件上传完成后需要设置File对象的属性(url，filename，relativePath），用于保存到数据库
            String url = new StringBuilder(getUriPrefix())
                    .append(StrPool.SLASH)
                    .append(properties.getBucketName())
                    .append(StrPool.SLASH)
                    .append(relativePath)
                    .append(StrPool.SLASH)
                    .append(fileName)
                    .toString();
            //替换掉windows环境的\路径
            url = StrUtil.replace(url, "\\\\", StrPool.SLASH);
            url = StrUtil.replace(url, "\\", StrPool.SLASH);
            file.setUrl(url);//  http://ip:port/oss-file-service/2020/05/xxx.doc
            file.setFilename(fileName);
            file.setRelativePath(relativePath);
        }

        /**
         * 文件删除
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            //拼接要删除的文件的绝对磁盘路径
            String filePath = Paths.get(properties.getEndpoint(), properties.getBucketName(), fileDeleteDO.getRelativePath(), fileDeleteDO.getFileName()).toString();
            java.io.File file = new java.io.File(filePath);
            FileUtils.deleteQuietly(file);
        }
    }

    /**
     * 本地分片文件策略处理类
     */
    @Service
    public class LocalChunkServiceImpl extends AbstractFileChunkStrategy{
        /**
         * 分片合并
         * @param files
         * @param fileName
         * @param fileChunksMergeDTO
         * @return
         */
        @Override
        protected R<File> merge(List<java.io.File> files, String fileName, FileChunksMergeDTO fileChunksMergeDTO) throws Exception{
            //加载配置文件相关信息
            FileServerProperties.Properties properties = fileServerProperties.getLocal();

            String endpoint = properties.getEndpoint(); // D:\\uploadFiles
            String bucketName = properties.getBucketName(); //oss-file-service

            //日期目录层次
            String format = LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_MONTH_FORMAT_SLASH));
            String relativePath = Paths.get(format).toString();

            //合并后文件存放目录
            String path = Paths.get(endpoint, bucketName, relativePath).toString();

            java.io.File uploadFolder = new java.io.File(path);
            //判断目录是否存在，如果不存在就需要创建
            if(!uploadFolder.exists()){
                uploadFolder.mkdirs();
            }

            //合并文件逻辑
            //创建合并后的文件对象
            java.io.File outputFile = new java.io.File(Paths.get(path,fileName).toString());
            if(!outputFile.exists()){
                //创建文件
                boolean newFile = outputFile.createNewFile();
                if(!newFile){
                    log.error("文件合并失败");
                    return R.fail("文件合并失败");
                }
                try(FileChannel outChannel = new FileOutputStream(outputFile).getChannel()){
                    for (java.io.File file : files) {//file对应的就是分片文件
                        try(FileChannel inChannel = new FileInputStream(file).getChannel()){
                            inChannel.transferTo(0,inChannel.size(),outChannel);
                        }catch (Exception ex){
                            log.error("分片文件合并失败");
                            return R.fail("分片文件合并失败");
                        }
                        //删除当前分片
                        file.delete();
                    }
                }catch (Exception ex){
                    log.error("分片文件合并失败");
                    return R.fail("分片文件合并失败");
                }
            }else {
                log.warn("文件[{}]已经存在",fileName);
            }

            //http://localhost:8080/2020/05/abc.avi
            String url = new StringBuilder(properties.getUriPrefix()).
                    append(bucketName).append(StrPool.SLASH).
                    append(relativePath).append(StrPool.SLASH).
                    append(fileName).toString();

            //分片合并成功，需要封装File对象相关属性
            File file = File.builder().relativePath(relativePath).url(StringUtils.replace(url, "\\", StrPool.SLASH)).build();
            return R.success(file);
        }
    }
}
