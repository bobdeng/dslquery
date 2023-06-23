package cn.beagile.dslquery;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface Operators {
    Map<String, Operator> OPERATORS = Stream.of(Operator.values())
            .collect(Collectors.toMap(Operator::getKeyword, Function.identity()));

    static Operator of(String condition) {
        return OPERATORS.get(condition);
    }
}
