package io.github.oldmanpushcart.moss.backend.ibatis.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class InstantTypeHandler extends BaseTypeHandler<Instant> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter.toEpochMilli());
    }

    @Override
    public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final var time = rs.getLong(columnName);
        return rs.wasNull() ? null : Instant.ofEpochMilli(time);
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final var time = rs.getLong(columnIndex);
        return rs.wasNull() ? null : Instant.ofEpochMilli(time);
    }

    @Override
    public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final var time = cs.getLong(columnIndex);
        return cs.wasNull() ? null : Instant.ofEpochMilli(time);
    }

}
