package com.itheima.pinda.file.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.base.id.IdGenerate;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.file.biz.FileBiz;
import com.itheima.pinda.file.dao.AttachmentMapper;
import com.itheima.pinda.file.domain.FileDO;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.dto.AttachmentResultDTO;
import com.itheima.pinda.file.dto.FilePageReqDTO;
import com.itheima.pinda.file.entity.Attachment;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.service.AttachmentService;
import com.itheima.pinda.file.strategy.FileStrategy;
import com.itheima.pinda.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 附件业务实现类
 */
@Service
@Slf4j
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper,Attachment> implements AttachmentService {
    @Autowired
    private FileStrategy fileStrategy;
    @Autowired
    private IdGenerate<Long> idGenerate;
    @Autowired
    private DozerUtils dozerUtils;
    @Autowired
    private FileServerProperties fileServerProperties;
    @Autowired
    private FileBiz fileBiz;
    /**
     * 文件上传
     * @param multipartFile
     * @param bizId
     * @param bizType
     * @param isSingle
     * @param id
     * @return
     */
    @Override
    public AttachmentDTO upload(MultipartFile multipartFile, Long bizId, String bizType, Boolean isSingle, Long id) {
        String bizIdStr = String.valueOf(bizId);
        //判断bizId是否为空，如果为空需要产生一个业务id
        if(bizId == null){
            bizIdStr = String.valueOf(idGenerate.generate());
        }

        //调用策略处理对象实现真正文件上传
        File file = fileStrategy.upload(multipartFile);

        //对象转换
        Attachment attachment = dozerUtils.map(file, Attachment.class);
        attachment.setBizId(bizIdStr);
        attachment.setBizType(bizType);

        LocalDateTime now = LocalDateTime.now();
        attachment.setCreateMonth(DateUtils.formatAsYearMonthEn(now));
        attachment.setCreateWeek(DateUtils.formatAsYearWeekEn(now));
        attachment.setCreateDay(DateUtils.formatAsDateEn(now));

        //判断当前业务是否是单一文件
        if(isSingle){
            //需要将当前业务下其他的文件信息从数据库删除
            super.remove(Wraps.<Attachment>lbQ().eq(Attachment::getBizId,bizIdStr).eq(Attachment::getBizType,bizType));
        }

        //完成文件上传后需要将文件信息保存到数据库中
        if(id != null && id > 0){
            attachment.setId(id);
            //执行数据库修改操作
            super.updateById(attachment);
        }else{
            //执行数据库新增操作
            attachment.setId(idGenerate.generate());
            super.save(attachment);
        }

        return dozerUtils.map(attachment,AttachmentDTO.class);
    }

    /**
     * 根据ids删除附件
     * @param ids
     */
    @Override
    public void remove(Long[] ids) {
        if(ArrayUtils.isEmpty(ids)){
            return;
        }
        //查询数据库，用于删除文件信息封装
        //select * from pd_attachment where id in (1,2)
        List<Attachment> list = super.list(Wrappers.<Attachment>lambdaQuery().in(Attachment::getId, ids));

        //从数据库中删除文件信息记录
        super.removeByIds(Arrays.asList(ids));

        //对象格式转换处理
        List<FileDeleteDO> fileDeleteDOList = list.stream().map((fi) ->
            FileDeleteDO.builder().relativePath(fi.getRelativePath())
                    .fileName(fi.getFilename())
                    .group(fi.getGroup())
                    .path(fi.getPath())
                    .build()
        ).collect(Collectors.toList());

        //删除文件
        fileStrategy.delete(fileDeleteDOList);
    }

    /**
     * 根据业务类型/业务id删除附件
     * @param bizId
     * @param bizType
     */
    @Override
    public void removeByBizIdAndBizType(String bizId, String bizType) {
        //根据业务类和业务id查询数据库
        List<Attachment> list = super.list(
                Wraps.<Attachment>lbQ()
                        .eq(Attachment::getBizId, bizId)
                        .eq(Attachment::getBizType, bizType));
        if (list.isEmpty()) {
            return;
        }

        //根据id删除文件
        remove(list.stream().mapToLong(
                Attachment::getId).boxed().toArray(Long[]::new));
    }

    /**
     * 根据ids打包下载附件
     * @param request
     * @param response
     * @param ids
     */
    @Override
    public void download(HttpServletRequest request, HttpServletResponse response, Long[] ids) throws Exception{
        //根据文件id查询数据库
        List<Attachment> list = (List<Attachment>) super.listByIds(Arrays.asList(ids));
        down(list,request,response);
    }

    /**
     * 根据业务类型/业务id打包下载附件
     * @param request
     * @param response
     * @param bizTypes
     * @param bizIds
     */
    @Override
    public void downloadByBiz(HttpServletRequest request, HttpServletResponse response, String[] bizTypes, String[] bizIds) throws Exception{
        //根据业务类型、业务id查询数据库
        List<Attachment> list = super.list(Wraps.<Attachment>lbQ().in(Attachment::getBizType, bizTypes).in(Attachment::getBizId, bizIds));
        down(list,request,response);
    }

    /**
     * 下载附件
     * @param list
     * @param request
     * @param response
     * @throws Exception
     */
    public void down(List<Attachment> list,HttpServletRequest request,HttpServletResponse response) throws Exception{
        if(list.isEmpty()){
            throw BizException.wrap("您下载的文件不存在!");
        }

        //对象转换
        List<FileDO> fileDOList = list.stream().map((file) -> FileDO.builder()
                .url(file.getUrl())
                .submittedFileName(file.getSubmittedFileName())
                .size(file.getSize())
                .dataType(file.getDataType())
                .build()
        ).collect(Collectors.toList());

        fileBiz.down(fileDOList,request,response);
    }

    /**
     * 查询附件分页数据
     *
     * @param page
     * @param data
     * @return
     */
    public IPage<Attachment> page(Page<Attachment> page, FilePageReqDTO data) {
        Attachment attachment = dozerUtils.map(data, Attachment.class);

        // ${ew.customSqlSegment} 语法一定要手动eq like 等 不能用lbQ!
        LbqWrapper<Attachment> wrapper = Wraps.<Attachment>lbQ()
                .like(Attachment::getSubmittedFileName, attachment.getSubmittedFileName())
                .like(Attachment::getBizType, attachment.getBizType())
                .like(Attachment::getBizId, attachment.getBizId())
                .eq(Attachment::getDataType, attachment.getDataType())
                .orderByDesc(Attachment::getId);
        return baseMapper.page(page, wrapper);
    }

    /**
     * 根据业务类型和业务id查询附件
     *
     * @param bizTypes
     * @param bizIds
     * @return
     */
    public List<AttachmentResultDTO> find(String[] bizTypes, String[] bizIds) {
        return baseMapper.find(bizTypes, bizIds);
    }
}