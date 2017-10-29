package _experimental;

import common.datastore.MinoOperationWithKey;
import common.parser.OperationWithKeyInterpreter;
import core.field.Field;
import core.field.FieldFactory;
import core.field.FieldView;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.srs.Rotate;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import searcher.pack.SeparableMinos;
import searcher.pack.SizedBit;
import searcher.pack.separable_mino.SeparableMino;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToIndexMain {
    public static void main(String[] args) throws IOException {
        SeparableMinos minos = SeparableMinos.createSeparableMinos(new MinoFactory(), new MinoShifter(), new SizedBit(10, 4));
        int size = minos.getMinos().size();
        System.out.println(size);

        ArrayList<SeparableMino> separableMinos = new ArrayList<>();
        separableMinos.addAll(minos.getMinos());

        Files.lines(Paths.get("input/result1"))
                .map(line -> {
                    Pattern pattern = Pattern.compile("\\d+\\.?\\d*");
                    String[] split = line.split("\\(");
                    Matcher matcher = pattern.matcher(split[1]);
                    if (!matcher.find())
                        throw new RuntimeException();
                    String support = matcher.group();

                    if (!matcher.find())
                        throw new RuntimeException();
                    String confidence = matcher.group();

                    List<SeparableMino> collect = Arrays.stream(line.split(" "))
                            .map(ToIndexMain::parseIndexOptional)
                            .filter(Optional::isPresent)
                            .map(o -> o.map(separableMinos::get))
                            .map(Optional::get)
                            .collect(Collectors.toList());
                    return new K(collect, Double.valueOf(support), Double.valueOf(confidence));
                })
                .filter(K::isNoDeleted)
                .filter(k -> k.confidence < 100.0)
                .sorted(Comparator.comparing(K::getConfidence).reversed())
                .forEach(k -> {
                    System.out.println(k.getSupport());
                    System.out.println(k.getConfidence());
                    Field field = k.getPreField();
                    System.out.println(FieldView.toString(field, 4));

                    System.out.println(" |");
                    System.out.println(" v");

                    SeparableMino y = k.getAfter();
                    Field fieldY = toField(y);
                    System.out.println(FieldView.toString(fieldY, 4));

                    System.out.println("===");
                });
    }

    private static Field toField(SeparableMino separableMino) {
        Field field = FieldFactory.createField(4);
        MinoOperationWithKey operationWithKey = separableMino.toMinoOperationWithKey();
        Mino mino = operationWithKey.getMino();
        field.put(mino, operationWithKey.getX(), separableMino.getLowerY() - mino.getMinY());
        field.insertWhiteLineWithKey(operationWithKey.getNeedDeletedKey());
        return field;
    }

    private static Optional<Integer> parseIndexOptional(String v) {
        try {
            return Optional.of(Integer.valueOf(v));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void main2(String[] args) throws IOException {
        SeparableMinos minos = SeparableMinos.createSeparableMinos(new MinoFactory(), new MinoShifter(), new SizedBit(10, 4));
        int size = minos.getMinos().size();
        System.out.println(size);

        MyFile outputIndexFile = new MyFile("output/index.csv");
        try (BufferedWriter writer = outputIndexFile.newBufferedWriter()) {
            for (SeparableMino mino : minos.getMinos()) {
                String line = toIndexLine(minos, mino);
                writer.write(line);
                writer.newLine();
            }
        }

        MinoFactory minoFactory = new MinoFactory();
        MyFile outputOperationFile = new MyFile("output/indexed_10x4S.csv");
        try (AsyncBufferedFileWriter writer = outputOperationFile.newAsyncWriter()) {
            Files.lines(Paths.get("input/result_10x4.csv")).parallel()
                    .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                    .filter(operationWithKeys -> {
                        // 左端のミノから順に探索する
                        operationWithKeys.sort(Comparator.comparingInt(o -> o.getX() + o.getMino().getMinX()));

                        MinoOperationWithKey first = operationWithKeys.get(0);
                        int maxX = first.getX() + first.getMino().getMaxX();
                        for (MinoOperationWithKey operationWithKey : operationWithKeys.subList(1, operationWithKeys.size())) {
                            int min = operationWithKey.getX() + operationWithKey.getMino().getMinX();
                            if (maxX < min)
                                return false;  // これまでの塊と独立して始まる  // 分割可能

                            int max = operationWithKey.getX() + operationWithKey.getMino().getMaxX();
                            if (maxX < max)
                                maxX = max;  // 右端を更新
                        }

                        return true;
                    })
                    .map(list -> list.stream()
                            .map(operation -> minos.toIndex(operation.getMino(), operation.getX(), operation.getY(), operation.getNeedDeletedKey()))
                            .map(Object::toString)
                            .collect(Collectors.joining(",")))
                    .forEach(writer::writeAndNewLine);
        }
    }

    private static String toIndexLine(SeparableMinos minos, SeparableMino mino) {
        MinoOperationWithKey operationWithKey = mino.toMinoOperationWithKey();
        long deleteKey = operationWithKey.getNeedDeletedKey();

        int d = 0;
        for (int index = 0; index < 4; index++) {
            long mask = 1L << (index * 10);
            if ((deleteKey & mask) != 0L)
                d += (1 << index);
        }

        int index = minos.toIndex(mino);
        String name = operationWithKey.getBlock().getName();
        String rotate = getRotate(operationWithKey.getRotate());
        int x = operationWithKey.getX();
        int lowerY = mino.getLowerY();
        String s = padding(Integer.toBinaryString(d));
        return String.format("%d,%s,%s,%d,%d,%s", index, name, rotate, x, lowerY, s);
    }

    private static String padding(String s) {
        int length = 4 - s.length();
        StringBuilder empty = new StringBuilder();
        for (int count = 0; count < length; count++)
            empty.append("0");
        return empty + s;
    }

    private static String getRotate(Rotate rotate) {
        switch (rotate) {
            case Spawn:
                return "0";
            case Left:
                return "L";
            case Right:
                return "R";
            case Reverse:
                return "2";
        }
        throw new IllegalStateException();
    }

    private static class K {
        private final List<SeparableMino> separableMinos;
        private final double support;
        private final double confidence;

        K(List<SeparableMino> separableMinos, double support, double confidence) {
            this.separableMinos = separableMinos;
            this.support = support;
            this.confidence = confidence;
        }

        boolean isNoDeleted() {
            return separableMinos.stream()
                    .skip(1)
                    .allMatch(separableMino -> separableMino.toMinoOperationWithKey().getNeedDeletedKey() == 0L);
        }

        Field getPreField() {
            Field merged = FieldFactory.createField(4);
            for (SeparableMino separableMino : separableMinos.subList(1, separableMinos.size())) {
                Field field = toField(separableMino);
                merged.merge(field);
            }
            return merged;
        }

        SeparableMino getAfter() {
            return separableMinos.get(0);
        }

        double getConfidence() {
            return confidence;
        }

        double getSupport() {
            return support;
        }
    }
}
