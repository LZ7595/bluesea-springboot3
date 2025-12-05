package com.example.backend.Service.back;

import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.back.ExpressBack;
import com.example.backend.Model.Vo.LabelList;

import java.util.List;

public interface ExpressBackService {
    PageResult<ExpressBack> search(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);
    int updateExpress(ExpressBack express);
    int addExpress(ExpressBack express);
    int deleteExpress(Integer express_id);
    int deleteExpressMore(List<Integer> expressIdList);
    List<LabelList> getList(Integer expressId, String keyword);
}
