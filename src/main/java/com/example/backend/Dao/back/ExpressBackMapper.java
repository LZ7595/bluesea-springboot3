package com.example.backend.Dao.back;


import com.example.backend.Model.Entity.back.ExpressBack;
import com.example.backend.Model.Vo.LabelList;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExpressBackMapper {
    @Select("<script>" +
            "SELECT * FROM express " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (chi LIKE CONCAT('%', #{searchKeyword}, '%') OR eng LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<ExpressBack> search(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) FROM express " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (chi LIKE CONCAT('%', #{searchKeyword}, '%') OR eng LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getTotalCount(Map<String, Object> params);

    @Update("UPDATE express SET eng = #{eng}, chi = #{chi}, image_url = #{image_url},update_time = #{update_time} WHERE id = #{id}")
    int updateExpress(ExpressBack express);

    @Insert("INSERT INTO express (eng, chi, image_url,create_time,update_time) VALUES (#{eng}, #{chi}, #{image_url},#{create_time},#{update_time})")
    int addExpress(ExpressBack express);

    @Delete("DELETE FROM express WHERE id = #{express_id}")
    int deleteExpress(Integer express_id);

    @Delete("<script>" +
            "DELETE FROM express WHERE id IN " +
            "<foreach item='id' collection='expressIdList' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteExpressMore(List<Integer> expressIdList);

    @Select("<script>" +
            "SELECT " +
            "    CONCAT(chi, ' (', eng, ')') AS label,  " + // chi和eng拼接，格式："中通快递 (zhongtong)"
            "    id AS value " +                          // id作为value
            "FROM express " +
            "<where>" +
            "    <if test='keyword != null and keyword != \"\"'>" +
            "        (chi LIKE CONCAT('%', #{keyword}, '%') " + // 匹配中文名称
            "         OR eng LIKE CONCAT('%', #{keyword}, '%')) " + // 匹配编码
            "    </if>" +
            "    <if test='expressId != null'>" +
            "        AND id = #{expressId} " + // 精准匹配ID
            "    </if>" +
            "</where>" +
            "ORDER BY id ASC " +
            "</script>")
    List<LabelList> getExpressList(@Param("keyword") String keyword, @Param("expressId") Integer expressId);
}
