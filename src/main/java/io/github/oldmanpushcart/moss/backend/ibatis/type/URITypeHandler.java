package io.github.oldmanpushcart.moss.backend.ibatis.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class URITypeHandler extends BaseTypeHandler<URI> {


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, URI parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public URI getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final var uriString = rs.getString(columnName);
        return uriString == null
                ? null
                : URI.create(uriString);
    }

    @Override
    public URI getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final var uriString = rs.getString(columnIndex);
        return uriString == null
                ? null
                : URI.create(uriString);
    }

    @Override
    public URI getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final var uriString = cs.getString(columnIndex);
        return uriString == null
                ? null
                : URI.create(uriString);
    }

}
