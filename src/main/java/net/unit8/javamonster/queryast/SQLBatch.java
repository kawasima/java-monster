package net.unit8.javamonster.queryast;

import net.unit8.javamonster.SQLJoinFunction;

public class SQLBatch {
    private String thisKey;
    private String parentKey;
    private SQLJoinFunction sqlJoin;

    private SQLBatch() { }

    public static class Builder {
        private SQLBatch sqlBatch;
        public Builder() {

        }

        public Builder thisKey(String thisKey) {
            sqlBatch.thisKey = thisKey;
            return this;
        }
        public Builder parentKey(String parentKey) {
            sqlBatch.parentKey = parentKey;
            return this;
        }
        public Builder sqlJoin(SQLJoinFunction sqlJoin) {
            sqlBatch.sqlJoin = sqlJoin;
            return this;
        }
        public SQLBatch build() {
            return sqlBatch;
        }
    }
}
