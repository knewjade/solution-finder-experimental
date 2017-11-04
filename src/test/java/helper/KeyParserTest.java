package helper;

import core.field.KeyOperators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


class KeyParserTest {
    @ParameterizedTest
    @CsvSource({
            "0, 0001",
            "1, 0010",
            "2, 0100",
            "3, 1000"
    })
    void parseToStringHeight4(int y, String expected) {
        long deleteKey = KeyOperators.getDeleteBitKey(y);
        assertThat(KeyParser.parseToString(deleteKey, 4)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1, 0011",
            "0, 2, 0101",
            "0, 3, 1001",
            "1, 2, 0110",
            "1, 3, 1010",
            "2, 3, 1100",
    })
    void parseToStringHeight4_2line(int y, int y2, String expected) {
        long deleteKey = KeyOperators.getDeleteBitKey(y) + KeyOperators.getDeleteBitKey(y2);
        assertThat(KeyParser.parseToString(deleteKey, 4)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1110",
            "1, 1101",
            "2, 1011",
            "3, 0111",
    })
    void parseToStringHeight4_3line(int noY, String expected) {
        long deleteKey = KeyOperators.getMaskForKeyBelowY(4) - KeyOperators.getDeleteBitKey(noY);
        assertThat(KeyParser.parseToString(deleteKey, 4)).isEqualTo(expected);
    }

    @Test
    void parseToStringHeight4_4line() {
        long deleteKey = KeyOperators.getMaskForKeyBelowY(4);
        assertThat(KeyParser.parseToString(deleteKey, 4)).isEqualTo("1111");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0001",
            "1, 0010",
            "2, 0100",
            "3, 1000"
    })
    void parseToLongHeight4(int y, String str) {
        long deleteKey = KeyOperators.getDeleteBitKey(y);
        assertThat(KeyParser.parseToLong(str)).isEqualTo(deleteKey);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1, 0011",
            "0, 2, 0101",
            "0, 3, 1001",
            "1, 2, 0110",
            "1, 3, 1010",
            "2, 3, 1100",
    })
    void parseToLongHeight4_2line(int y, int y2, String str) {
        long deleteKey = KeyOperators.getDeleteBitKey(y) + KeyOperators.getDeleteBitKey(y2);
        assertThat(KeyParser.parseToLong(str)).isEqualTo(deleteKey);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1110",
            "1, 1101",
            "2, 1011",
            "3, 0111",
    })
    void parseToLongHeight4_3line(int noY, String str) {
        long deleteKey = KeyOperators.getMaskForKeyBelowY(4) - KeyOperators.getDeleteBitKey(noY);
        assertThat(KeyParser.parseToLong(str)).isEqualTo(deleteKey);
    }

    @Test
    void parseToLongHeight4_4line() {
        long deleteKey = KeyOperators.getMaskForKeyBelowY(4);
        assertThat(KeyParser.parseToLong("1111")).isEqualTo(deleteKey);
    }
}