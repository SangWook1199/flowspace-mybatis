package com.flowspace.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

// 번호 목록 → JSON 배열 문자열 ([1,2,3], 프로시저에서 JSON_TABLE로 펼침)
public class IdListTypeHandler extends BaseTypeHandler<List<Long>> {

    // 인자 설정
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter, JdbcType jdbcType)
        throws SQLException {
        ps.setString(i, parameter.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public List<Long> getNullableResult(ResultSet rs, String columnName) {
        throw new UnsupportedOperationException();
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public List<Long> getNullableResult(ResultSet rs, int columnIndex) {
        throw new UnsupportedOperationException();
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public List<Long> getNullableResult(CallableStatement cs, int columnIndex) {
        throw new UnsupportedOperationException();
    }
}
