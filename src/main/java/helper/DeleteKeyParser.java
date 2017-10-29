package helper;

public class DeleteKeyParser {
    public static String parse(long deleteKey, int height) {
        int intValue = 0;
        for (int index = 0; index < 4; index++) {
            long mask = 1L << (index * 10);
            if ((deleteKey & mask) != 0L)
                intValue += (1 << index);
        }
        return padding(Integer.toBinaryString(intValue), height);
    }

    private static String padding(String s, int maxLength) {
        int length = maxLength - s.length();
        StringBuilder empty = new StringBuilder();
        for (int count = 0; count < length; count++)
            empty.append("0");
        return empty + s;
    }
}
