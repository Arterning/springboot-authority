package com.itheima.pinda.file.controller;


import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.domain.FileStatisticsDO;
import com.itheima.pinda.file.dto.FileOverviewDTO;
import com.itheima.pinda.file.dto.FileStatisticsAllDTO;
import com.itheima.pinda.file.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件统计接口
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@Api(value = "Statistics", tags = "统计接口")
public class StatisticsController extends BaseController {
    @Autowired
    private FileService fileService;

    @ApiOperation(value = "云盘首页数据概览", notes = "云盘首页数据概览")
    @GetMapping(value = "/overview")
    public R<FileOverviewDTO> overview(@RequestParam(name = "userId", required = false) Long userId) {
        return success(fileService.findOverview(userId, null, null));
    }

    @ApiOperation(value = "按照类型，统计各种类型的 大小和数量", notes = "按照类型，统计当前登录人各种类型的大小和数量")
    @GetMapping(value = "/type")
    public R<List<FileStatisticsDO>> findAllByDataType(@RequestParam(name = "userId", required = false) Long userId) {
        return success(fileService.findAllByDataType(userId));
    }

    @ApiOperation(value = "按照时间统计各种类型的文件的数量和大小", notes = "按照时间统计各种类型的文件的数量和大小 不指定时间，默认查询一个月")
    @GetMapping(value = "")
    public R<FileStatisticsAllDTO> findNumAndSizeToTypeByDate(@RequestParam(name = "userId", required = false) Long userId,
                                                              @RequestParam(value = "startTime", required = false) LocalDateTime startTime,
                                                              @RequestParam(value = "endTime", required = false) LocalDateTime endTime) {
        return success(fileService.findNumAndSizeToTypeByDate(userId, startTime, endTime));
    }
}