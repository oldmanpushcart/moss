package io.github.oldmanpushcart.moss.backend.ibatis.type;

import io.github.oldmanpushcart.moss.util.FloatVector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FloatVectorTypeHandler extends BaseTypeHandler<FloatVector> {

    private static final ByteOrder order = ByteOrder.nativeOrder();

    private static byte[] toByteArray(float[] vector) {
        if (vector == null) {
            return null;
        }
        final var bytes = new byte[vector.length * Float.BYTES];
        ByteBuffer.wrap(bytes)
                .order(order)
                .asFloatBuffer()
                .put(vector);
        return bytes;
    }

    private static float[] toFloatArray(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        final var array = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes)
                .order(order)
                .asFloatBuffer()
                .get(array);
        return array;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FloatVector parameter, JdbcType jdbcType) throws SQLException {
        ps.setBytes(i, toByteArray(parameter.getData()));
    }

    @Override
    public FloatVector getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final var bytes = rs.getBytes(columnName);
        return rs.wasNull() ? null : new FloatVector(toFloatArray(bytes));
    }

    @Override
    public FloatVector getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final var bytes = rs.getBytes(columnIndex);
        return rs.wasNull() ? null : new FloatVector(toFloatArray(bytes));
    }

    @Override
    public FloatVector getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final var bytes = cs.getBytes(columnIndex);
        return cs.wasNull() ? null : new FloatVector(toFloatArray(bytes));
    }
}
