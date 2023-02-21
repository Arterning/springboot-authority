package com.itheima.pinda.file.strategy;

import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
/**
 * 最高层策略处理接口
 */
public interface FileStrategy {
    /**
     * 上传文件
     * @param file
     * @return
     */
    public File upload(MultipartFile file);

    /**
     * 文件删除
     * @param list
     * @return
     */
    public boolean delete(List<FileDeleteDO> list);
}
