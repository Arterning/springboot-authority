package com.itheima.pinda.file.strategy;

import com.itheima.pinda.base.R;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;

/**
 * 最高层文件分片处理策略接口
 */
public interface FileChunkStrategy {
    /**
     * 分片合并方法
     * @param fileChunksMergeDTO
     * @return
     */
    public R<File> chunkMerge(FileChunksMergeDTO fileChunksMergeDTO);
}
