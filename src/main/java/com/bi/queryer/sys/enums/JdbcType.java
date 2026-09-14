package com.bi.queryer.sys.enums;

import java.sql.Types;

public enum JdbcType {
    BIT(Types.BIT),
    TINYINT(Types.TINYINT),

    SMALLINT(Types.SMALLINT),

    INTEGER(Types.INTEGER),

    BIGINT(Types.BIGINT),

    FLOAT(Types.FLOAT),

    REAL(Types.REAL),

    DOUBLE(Types.DOUBLE),

    NUMERIC(Types.NUMERIC),

    DECIMAL(Types.DECIMAL),

    CHAR(Types.CHAR),

    VARCHAR(Types.VARCHAR),

    LONGVARCHAR(Types.LONGVARCHAR),

    DATE(Types.DATE),

    TIME(Types.TIME),

    TIMESTAMP(Types.TIMESTAMP),

    BINARY(Types.BINARY),

    VARBINARY(Types.VARBINARY),

    LONGVARBINARY(Types.LONGVARBINARY),

    BLOB(Types.BLOB),

    CLOB(Types.CLOB),

    BOOLEAN(Types.BOOLEAN),

    NVARCHAR(Types.NVARCHAR),

    NCHAR(Types.NCHAR),

    NCLOB(Types.NCLOB),

    LONGNVARCHAR(Types.LONGNVARCHAR),

    NULL(Types.NULL);


    private int value;

    private JdbcType(int value){
        this.value = value;
    }

    public static JdbcType get(String typeStr){
        for(JdbcType t : JdbcType.values()){
            if(t.toString().equalsIgnoreCase(typeStr)){
                return t;
            }
        }

        return VARCHAR;
    }

    public static JdbcType get(int value){
        for(JdbcType t : JdbcType.values()){
            if(t.value == value){
                return t;
            }
        }

        return VARCHAR;
    }
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
