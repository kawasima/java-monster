package net.unit8.javamonster.stringifiers.dialects;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

public class PaginationOption implements Serializable {
    private String joinCondition;
    private String joinType;
    private String extraJoinName;
    private String extraJoinAs;
    private String extraJoinCondition;
    private Function<String, String> quoteFunc;

    private PaginationOption() {

    }
    public Optional<String> getJoinCondition() {
        return Optional.ofNullable(joinCondition);
    }

    public Optional<String> getJoinType() {
        return Optional.ofNullable(joinType);
    }

    public Optional<String> getExtraJoin() {
        return Optional.ofNullable(extraJoinName);
    }

    public Function<String, String> getQuoteFunc() {
        return Optional.ofNullable(quoteFunc)
                .orElse(s -> "\"" + s + "\"");
    }

    public static class Builder {
        private PaginationOption option;
        public Builder() {
            option = new PaginationOption();
        }

        public Builder joinCondition(String joinCondition) {
            option.joinCondition = joinCondition;
            return this;
        }

        public Builder joinType(String joinType) {
            option.joinType = joinType;
            return this;
        }

        public Builder extraJoin(String name, String as, String condition) {
            option.extraJoinName = name;
            option.extraJoinAs = as;
            option.extraJoinCondition = condition;
            return this;
        }

        public PaginationOption build() {
            return option;
        }
    }
}
