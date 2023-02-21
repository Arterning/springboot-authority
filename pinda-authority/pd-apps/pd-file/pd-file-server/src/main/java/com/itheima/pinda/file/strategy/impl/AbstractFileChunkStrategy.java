package com.itheima.pinda.file.strategy.impl;

import com.itheima.pinda.base.R;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.enumeration.IconType;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.service.FileService;
import com.itheima.pinda.file.strategy.FileChunkStrategy;
import com.itheima.pinda.file.utils.FileDataTypeUtil;
import com.itheima.pinda.file.utils.FileLock;
import com.itheima.pinda.utils.DateUtils;
import com.itheima.pinda.utils.NumberHelper;
import com.itheima.pinda.utils.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * 文件分片处理 抽象策略类
 */
@Slf4j
public abstract class AbstractFileChunkStrategy implements FileChunkStrategy{
    @Autowired
    private FileService fileService;
    @Autowired
    protected FileServerProperties fileServerProperties;

    /**
     * 分片合并---处理主要流程
     * @param fileChunksMergeDTO
     * @return
     */
    @Override
    public R<File> chunkMerge(FileChunksMergeDTO fileChunksMergeDTO) {
        //定义文件合并后的文件名称
        String fileName = fileChunksMergeDTO.getName() + StrPool.DOT + fileChunksMergeDTO.getExt();

        //分片合并
        R<File> result = this.chunkMerge(fileChunksMergeDTO, fileName);

        //合并后将文件信息保存到数据库
        if(result.getIsSuccess() && result.getData() != null){
            //合并分片成功，保存数据库
            File filePo = result.getData();
            //设置文件对象的属性，这些属性对应都需要保存到数据库
            LocalDateTime now = LocalDateTime.now();
            filePo.setDataType(FileDataTypeUtil.getDataType(fileChunksMergeDTO.getContextType()))
                    .setCreateMonth(DateUtils.formatAsYearMonthEn(now))
                    .setCreateWeek(DateUtils.formatAsYearWeekEn(now))
                    .setCreateDay(DateUtils.formatAsDateEn(now))
                    .setSubmittedFileName(fileChunksMergeDTO.getSubmittedFileName())
                    .setIsDelete(false)
                    .setSize(fileChunksMergeDTO.getSize())
                    .setFileMd5(fileChunksMergeDTO.getMd5())
                    .setContextType(fileChunksMergeDTO.getContextType())
                    .setFilename(fileName)
                    .setExt(fileChunksMergeDTO.getExt())
                    .setIcon(IconType.getIcon(fileChunksMergeDTO.getExt()).getIcon());

            fileService.save(filePo);
            return R.success(filePo);
        }

        return result;
    }

    /**
     * 分片合并
     * @param fileChunksMergeDTO
     * @param fileName
     * @return
     */
    public R<File> chunkMerge(FileChunksMergeDTO fileChunksMergeDTO,String fileName){
        //配置文件中配置的分片文件存储目录 D:\\chunks
        String storagePath = fileServerProperties.getStoragePath();
        //获得分片文件存储的路径 D:\\chunks\\2020\\05
        String path = FileDataTypeUtil.getUploadPathPrefix(storagePath);
        //分片数量
        Integer chunks = fileChunksMergeDTO.getChunks();
        String md5 = fileChunksMergeDTO.getMd5();
        String folder = fileChunksMergeDTO.getName();

        //根据指定的目录获取文件的数量
        int chunksNum = this.getChunksNum(Paths.get(path, folder).toString());

        //检查分片数量是否足够
        if(chunks == chunksNum){
            //数量足够，可以进行分片的合并操作
            Lock lock = FileLock.getLock(folder);
            try {
                lock.lock();
                //获得所有分片文件
                List<java.io.File> chunkFiles = this.getChunks(Paths.get(path, folder).toString());

                //对chunkFiles集合中的分片文件进行排序
                chunkFiles.sort((f1,f2) ->  NumberHelper.intValueOf0(f1.getName()) - NumberHelper.intValueOf0(f2.getName())
                );

                //调用子类具体分片合并方式实现分片的合并
                R<File> result = this.merge(chunkFiles,fileName,fileChunksMergeDTO);

                //清理分片文件、目录、临时文件
                this.cleanSpace(folder,path);

                return result;
            }catch (Exception ex){
                log.error("分片合并失败");
                return R.fail("分片合并失败");
            }finally {
                //释放锁
                lock.unlock();
                //清理锁对象
                FileLock.removeLock(folder);
            }
        }
        log.error("分片数量不对，无法进行分片合并");
        return R.fail("分片数量不对，无法进行分片合并");
    }

    /**
     * 根据指定目录返回对应的文件数量
     * @param path
     * @return
     */
    public int getChunksNum(String path){//D:\\chunks\\2020\\05\\abc
        java.io.File folder = new java.io.File(path);
        java.io.File[] files = folder.listFiles((file) -> {
            if(file.isDirectory()){
                return false;
            }
            return true;
        });
        return files.length;
    }

    /**
     * 获得指定目录下所有的文件
     * @param path
     * @return
     */
    public List<java.io.File> getChunks(String path){//D:\\chunks\\2020\\05\\abc
        java.io.File folder = new java.io.File(path);
        java.io.File[] files = folder.listFiles((file) -> {
            if(file.isDirectory()){
                return false;
            }
            return true;
        });
        return new ArrayList<>(Arrays.asList(files));
    }

    /**
     * 分片合并抽象方法，需要子类实现
     * @param files
     * @param fileName
     * @param fileChunksMergeDTO
     * @return
     */
    protected abstract R<File> merge(List<java.io.File> files, String fileName, FileChunksMergeDTO fileChunksMergeDTO) throws Exception;

    /**
     * 清理分片相关文件
     * @param folder
     * @param path
     */
    public void cleanSpace(String folder,String path){
        //删除存放分片文件的目录
        java.io.File chunkFolder = new java.io.File(Paths.get(path,folder).toString());
        FileUtils.deleteQuietly(chunkFolder);
        //删除.tmp临时文件
        java.io.File tmpFile = new java.io.File(Paths.get(path,folder + ".tmp").toString());
        FileUtils.deleteQuietly(tmpFile);
    }
}
