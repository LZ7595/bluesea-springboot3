package com.example.backend.Impl.back;

import com.example.backend.Dao.back.ExpressBackMapper;
import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.back.ExpressBack;
import com.example.backend.Model.Vo.LabelList;
import com.example.backend.Service.back.ExpressBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpressBackServiceImpl implements ExpressBackService {

    @Autowired
    private ExpressBackMapper expressBackMapper;
    @Override
    public PageResult<ExpressBack> search(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("searchKeyword", searchKeyword);
        params.put("sortField", sortField);
        params.put("sortOrder", sortOrder);
        params.put("offset", (currentPage - 1) * pageSize);
        params.put("pageSize", pageSize);
        List<ExpressBack> expressBackList = expressBackMapper.search(params);
        Map<String,Object> countParams = new HashMap<>();
        countParams.put("searchKeyword", searchKeyword);
        int total = expressBackMapper.getTotalCount(countParams);
        return new PageResult<>(expressBackList, total, currentPage, pageSize, (int) Math.ceil((double) total / pageSize));
    }

    @Override
    public int updateExpress(ExpressBack express) {
        express.setUpdate_time(new Date());
        return expressBackMapper.updateExpress(express);
    }
    @Override
    public int addExpress(ExpressBack express) {
        express.setCreate_time(new Date());
        express.setUpdate_time(new Date());
        return expressBackMapper.addExpress(express);
    }
    @Override
    public int deleteExpress(Integer express_id) {
        return expressBackMapper.deleteExpress(express_id);
    }
    @Override
    public int deleteExpressMore(List<Integer> expressIdList) {
        return expressBackMapper.deleteExpressMore(expressIdList);
    }
    @Override
    public List<LabelList> getList(Integer expressId, String keyword) {
        return expressBackMapper.getExpressList(keyword,expressId);
    }
}
