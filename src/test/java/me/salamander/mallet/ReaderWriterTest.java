package me.salamander.mallet;

import me.salamander.mallet.shaders.annotation.NullableType;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.util.Ref;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ReaderWriterTest {
    private static final TestStruct[] testData = new TestStruct[1000];
    private static final MalletContext context = new MalletContext();

    @BeforeAll
    public static void setUp() {
        long seed = (new Random()).nextLong();
        System.out.println("Seed: " + seed);
        Random random = new Random(seed);

        for (int i = 0; i < testData.length; i++) {
            testData[i] = TestStruct.makeRandom(random);
        }
    }

    @Test
    public void basicTest() {
        Type type = Type.getType(TestStruct.class);
        MalletType malletType = context.getType(type);

        ByteBuffer buffer = ByteBuffer.allocate(testData.length * malletType.getSize());
        BiConsumer<ByteBuffer, TestStruct> writer = malletType.makeWriter(BiConsumer.class);

        for (TestStruct testDatum : testData) {
            writer.accept(buffer, testDatum);
        }

        buffer.flip();

        BiConsumer<ByteBuffer, Consumer> reader = malletType.makeReader(Consumer.class, "this");

        for (int i = 0; i < testData.length; i++) {
            int finalI = i;
            reader.accept(buffer, (testStruct) -> {
                assertEquals(testData[finalI], testStruct);
            });
        }
    }

    @Test
    public void splitTest1() {
        Type type = Type.getType(TestStruct.class);
        MalletType malletType = context.getType(type);

        ByteBuffer buffer = ByteBuffer.allocate(testData.length * malletType.getSize());
        BiConsumer<ByteBuffer, TestStruct> writer = malletType.makeWriter(BiConsumer.class);

        for (TestStruct testDatum : testData) {
            writer.accept(buffer, testDatum);
        }

        buffer.flip();

        BiConsumer<ByteBuffer, TestStructVisitor> reader = malletType.makeReader(TestStructVisitor.class, "this.x", "this.vec.x", "this.vec.y", "this.vec.z", "this.y", "this.magic");

        for (int i = 0; i < testData.length; i++) {
            int finalI = i;
            reader.accept(buffer, (x, vecX, vecY, vecZ, y, magic) -> {
                assertEquals(testData[finalI].x, x);
                assertEquals(testData[finalI].vec.x, vecX);
                assertEquals(testData[finalI].vec.y, vecY);
                assertEquals(testData[finalI].vec.z, vecZ);
                assertEquals(testData[finalI].y, y);
                assertEquals(testData[finalI].magic, magic);
            });
        }
    }

    public enum MagicEnum {
        MAGIC_NUMBER_1(4627),
        MAGIC_NUMBER_2(19309),
        MAGIC_NUMBER_3(392);

        private int value;

        MagicEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @NullableType
    public static class TestStruct {
        public final int x;
        public final Vector3f vec;
        public final float y;
        public final MagicEnum magic;

        public TestStruct(int x, Vector3f vec, float y, MagicEnum magic) {
            this.x = x;
            this.vec = vec;
            this.y = y;
            this.magic = magic;
        }

        public static TestStruct makeRandom(Random random) {
            return new TestStruct(random.nextInt(), new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()), random.nextFloat(), MagicEnum.values()[random.nextInt(MagicEnum.values().length)]);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestStruct that = (TestStruct) o;
            return x == that.x && Float.compare(that.y, y) == 0 && Objects.equals(vec, that.vec) && magic == that.magic;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, vec, y, magic);
        }

        @Override
        public String toString() {
            return "TestStruct{" +
                    "x=" + x +
                    ", vec=" + vec +
                    ", y=" + y +
                    ", magic=" + magic +
                    '}';
        }
    }

    public static interface TestStructVisitor {
        void visit(int x, float v_x, float v_y, float v_z, float y, MagicEnum magic);
    }
}
