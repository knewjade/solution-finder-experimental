package helper;

import core.field.KeyOperators;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


class DeleteKeyParserTest {
    @ParameterizedTest
    @CsvSource({"0, 0001", "1, 0010", "2, 0100", "3, 1000"})
    void parseHeight4(int y, String expected) {
        long deleteKey = KeyOperators.getDeleteBitKey(y);
        assertThat(DeleteKeyParser.parse(deleteKey, 4)).isEqualTo(expected);
    }
}