package com.itheima.pinda.file.storage;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileChunkStrategy;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * FASTDFS配置
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type",havingValue = "FAST_DFS")
public class FastDfsAutoConfigure {
    /**
     * FASTDfs文件策略处理类
     */
    @Service
    public class FastDfsServiceImpl extends AbstractFileStrategy{
        //注入操作FastDfs的客户端对象
        @Autowired
        private FastFileStorageClient storageClient;
        /**
         * 上传文件
         * @param file
         * @param multipartFile
         * @throws Exception
         */
        @Override
        public void uploadFile(File file, MultipartFile multipartFile) throws Exception {
            //调用FastDFS客户端对象将文件上传到FastDFS
            StorePath storePath = storageClient.uploadFile(multipartFile.getInputStream(), multipartFile.getSize(), file.getExt(), null);

            //文件上传完成后需要设置上传文件的相关信息，用于保存到数据库
            file.setUrl(fileProperties.getUriPrefix() + storePath.getFullPath());//a/b/c.doc
            file.setGroup(storePath.getGroup());
            file.setPath(storePath.getPath());
        }

        /**
         * 删除文件
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            //调用FastDFS的客户端对象实现文件删除
            storageClient.deleteFile(fileDeleteDO.getGroup(),fileDeleteDO.getPath());
        }
    }

    /**
     * FastDFS分片文件策略处理类
     */
    @Service
    public class FastDfsChunkServiceImpl extends AbstractFileChunkStrategy{
        @Autowired
        private AppendFileStorageClient storageClient;
        /**
         * 分片合并
         * @param files
         * @param fileName
         * @param fileChunksMergeDTO
         * @return
         * @throws Exception
         */
        @Override
        protected R<File> merge(List<java.io.File> files, String fileName, FileChunksMergeDTO fileChunksMergeDTO) throws Exception {
            StorePath storePath = null;
            for(int i=0;i<files.size();i++){
                java.io.File chunkFile = files.get(i);
                if(i == 0){
                    storePath = storageClient.uploadAppenderFile(null,FileUtils.openInputStream(chunkFile),chunkFile.length(),fileChunksMergeDTO.getExt());
                }else{
                    storageClient.appendFile(storePath.getGroup(),storePath.getPath(),FileUtils.openInputStream(chunkFile),chunkFile.length());
                }
            }

            if(storePath == null){
                //分片合并失败
                log.error("分片合并失败");
                return R.fail("分片合并失败");
            }

            // 合并成功后需要封装File对象返回
            String url = new StringBuilder(fileServerProperties.getUriPrefix())
                    .append(storePath.getFullPath())
                    .toString();
            File filePo = File.builder()
                    .url(url)
                    .group(storePath.getGroup())
                    .path(storePath.getPath())
                    .build();
            return R.success(filePo);
        }
    }
}
