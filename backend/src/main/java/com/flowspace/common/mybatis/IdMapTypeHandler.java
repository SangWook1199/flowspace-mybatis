package com.flowspace.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

// 번호 쌍 → JSON 배열 문자열 ([[원본, 새], ...], 프로시저에서 JSON_TABLE로 펼침)
public class IdMapTypeHandler extends BaseTypeHandler<Map<Long, Long>> {

    // 인자 설정
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<Long, Long> parameter, JdbcType jdbcType)
        throws SQLException {
        ps.setString(i, parameter.entrySet().stream().map(e -> "[" + e.getKey() + "," + e.getValue() + "]")
            .collect(Collectors.joining(",", "[", "]")));
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public Map<Long, Long> getNullableResult(ResultSet rs, String columnName) {
        throw new UnsupportedOperationException();
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public Map<Long, Long> getNullableResult(ResultSet rs, int columnIndex) {
        throw new UnsupportedOperationException();
    }

    // 조회 결과 변환 (사용 안 함)
    @Override
    public Map<Long, Long> getNullableResult(CallableStatement cs, int columnIndex) {
        throw new UnsupportedOperationException();
    }
}
