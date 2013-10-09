package org.n3r.diamond.server.domain;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PageHelper {
    @SuppressWarnings("unchecked")
    public static <E> Page<E> fetchPage(String driverName, JdbcTemplate jt,
                             String sqlCountRows,
                             String sqlFetchRows,
                             Object args[],
                             int pageNo,
                             int pageSize,
                             final ParameterizedRowMapper<E> rowMapper) {
        if (pageSize == 0) return null;

        int rowCount = jt.queryForInt(sqlCountRows, args);

        int pageCount = rowCount / pageSize;

        if (rowCount > pageSize * pageCount) pageCount++;
        if (pageNo > pageCount) return null;

        final Page<E> page = new Page<E>();
        page.setPageNo(pageNo);
        page.setTotalPages(pageCount);
        page.setTotalCount(rowCount);


        int startRow = (pageNo - 1) * pageSize;
        String selectSQL = createPageSql(driverName, sqlFetchRows,startRow, pageSize);
        jt.query(selectSQL, args, new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                final List<E> pageItems = page.getPageItems();
                int currentRow = 0;
                while (rs.next()) {
                    pageItems.add(rowMapper.mapRow(rs, currentRow++));
                }
                return page;
            }
        });

        return page;
    }


    public static String createPageSql(String driverName, String sql, int startRow, int pageSize ) {
        if (StringUtils.containsIgnoreCase(driverName, "oracle"))
            return createOraclePageSql(sql, startRow, pageSize);

        if (StringUtils.containsIgnoreCase(driverName, "mysql"))
            return createMySqlPageSql(sql, startRow, pageSize);

        return sql;
    }

    private static String createMySqlPageSql(String sql, int startRow, int pageSize) {
        return sql + " LIMIT " + startRow + "," + pageSize;
    }

    private static String createOraclePageSql(String sql, int startRow, int pageSize) {
         return "SELECT * FROM ( SELECT ROW__.*, ROWNUM RN__ FROM ( " + sql
                + " ) ROW__  WHERE ROWNUM <= " + (startRow + pageSize) + ") WHERE RN__ >= " + startRow;
    }
}