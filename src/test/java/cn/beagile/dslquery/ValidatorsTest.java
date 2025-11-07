package cn.beagile.dslquery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorsTest {
    @ParameterizedTest
    @CsvSource(
            {
                    "info.performances.totalPostCount30d",
                    "abc",
                    "ab.ce"
            }
    )
    void 字段合法(String field) {
        assertDoesNotThrow(() -> Validators.validateFieldName(field));

    }
}
