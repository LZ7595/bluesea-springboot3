package com.example.backend.Controller.back;

import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.back.ExpressBack;
import com.example.backend.Model.Vo.LabelList;
import com.example.backend.Service.back.ExpressBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/back/express")
public class ExpressBackController {

    @Autowired
    private ExpressBackService expressBackService;

    @GetMapping("/search")
    public PageResult<ExpressBack> search(@RequestParam(required = false) String searchKeyword,
                                          @RequestParam(defaultValue = "create_time") String sortField,
                                          @RequestParam(defaultValue = "DESC") String sortOrder,
                                          @RequestParam(defaultValue = "1") int currentPage,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        return expressBackService.search(searchKeyword,sortField, sortOrder, currentPage, pageSize);

    }

    @GetMapping("/list/{expressId}")
    public List<LabelList> getList(@PathVariable Integer expressId, @RequestParam(required = false) String keyword) {
        try {
            return expressBackService.getList(expressId, keyword); // 服务层方法新增参数
        } catch (Exception e) {
            throw new RuntimeException("获取列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public List<LabelList> getList(@RequestParam(required = false) String keyword) {
        try {
            return expressBackService.getList(null,keyword); // 服务层方法新增参数
        } catch (Exception e) {
            throw new RuntimeException("获取列表失败: " + e.getMessage());
        }
    }
    @PutMapping("/update")
    public String updateExpress(@RequestBody ExpressBack express) {
        int result = expressBackService.updateExpress(express);
        if (result > 0) {
            return "快递信息修改成功";
        } else {
            return "快递信息修改失败";
        }
    }
    @PostMapping("/add")
    public String addExpress(@RequestBody ExpressBack express) {
        int result = expressBackService.addExpress(express);
        if (result > 0) {
            return "快递信息添加成功";
        } else {
            return "快递信息添加失败";
        }
    }
    @DeleteMapping("/delete")
    public String deleteExpress(@RequestParam Integer express_id) {
        int result = expressBackService.deleteExpress(express_id);
        if (result > 0) {
            return "快递信息删除成功";
        } else {
            return "快递信息删除失败";
        }
    }

    @DeleteMapping("/deleteMore")
    public String deleteExpressMore(@RequestBody List<Integer> expressIdList) {
        int result = expressBackService.deleteExpressMore(expressIdList);
        if (result > 0) {
            return "快递信息批量删除成功";
        } else
            return "快递信息批量删除失败";
        }
    }
