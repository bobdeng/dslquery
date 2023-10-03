package cn.beagile.dslquery;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface Operators {
    Map<String, Operator> OPERATORS = Stream.of(Operator.values())
            .collect(Collectors.toMap(operator -> operator.keyword, Function.identity()));
    Map<String, Operator> ALIAS = Stream.of(Operator.values())
            .collect(Collectors.toMap(operator -> operator.abbr, Function.identity()));

    static Operator byName(String operatorName) {
        return Optional.ofNullable(ALIAS.get(operatorName))
                .orElseGet(() -> OPERATORS.get(operatorName));
    }
}
