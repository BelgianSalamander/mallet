package me.salamander.mallet.globject.vao;

import me.salamander.mallet.globject.buffer.BufferObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

public class VAOLayout {
    private final LayoutElement[] elements;
    private final int[] indices;
    private final int[] strides;

    public VAOLayout(LayoutElement[] elements, int... strides) {
        this.elements = elements;
        this.indices = new int[elements.length];
        this.strides = strides;

        Arrays.sort(elements, Comparator.comparingInt(LayoutElement::bufferIndex));

        for (int i = 0; i < elements.length; i++) {
            indices[i] = elements[i].index();
        }
    }

    public void declareLayout(BufferObject... buffers) {
        int currIndex = -1;

        for (LayoutElement element : elements) {
            if (element.bufferIndex() != currIndex) {
                currIndex = element.bufferIndex();
                buffers[currIndex].bind(GL_ARRAY_BUFFER);
            }

            element.declare(strides[currIndex]);
        }
    }

    public void enable() {
        for (int index : indices) {
            glEnableVertexAttribArray(index);
        }
    }

    private interface LayoutElement {
        void declare(int stride);
        int index();
        int bufferIndex();
    }

    private static abstract class AbstractLayoutElement implements LayoutElement {
        protected final int index;
        protected final int bufferIndex;
        protected final int offset;

        public AbstractLayoutElement(int index, int bufferIndex, int offset) {
            this.index = index;
            this.bufferIndex = bufferIndex;
            this.offset = offset;
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int bufferIndex() {
            return bufferIndex;
        }
    }

    private static class FloatLayoutElement extends AbstractLayoutElement {
        private final boolean normalized;
        private final int numFloats;

        public FloatLayoutElement(int index, int bufferIndex, int offset, int numFloats, boolean normalized) {
            super(index, bufferIndex, offset);
            this.normalized = normalized;
            this.numFloats = numFloats;
        }

        @Override
        public void declare(int stride) {
            glVertexAttribPointer(index, numFloats, GL_FLOAT, normalized, stride, offset);
        }
    }

    private static class IntLayoutElement extends AbstractLayoutElement {
        private final int numInts;

        public IntLayoutElement(int index, int bufferIndex, int offset, int numInts) {
            super(index, bufferIndex, offset);
            this.numInts = numInts;
        }

        @Override
        public void declare(int stride) {
            glVertexAttribIPointer(index, numInts, GL_INT, stride, offset);
        }
    }

    private static class DoubleLayoutElement extends AbstractLayoutElement {
        private final int numDoubles;

        public DoubleLayoutElement(int index, int bufferIndex, int offset, int numDoubles) {
            super(index, bufferIndex, offset);
            this.numDoubles = numDoubles;
        }

        @Override
        public void declare(int stride) {
            glVertexAttribLPointer(index, numDoubles, GL_DOUBLE, stride, offset);
        }
    }

    private static class ByteLayoutElement extends AbstractLayoutElement {
        private final int numBytes;

        public ByteLayoutElement(int index, int bufferIndex, int offset, int numBytes) {
            super(index, bufferIndex, offset);
            this.numBytes = numBytes;
        }

        @Override
        public void declare(int stride) {
            glVertexAttribPointer(index, numBytes, GL_BYTE, false, stride, offset);
        }
    }

    public static class PackedLayoutBuilder {
        private final List<LayoutElement> elements = new ArrayList<>();
        private int size = 0;

        public VAOLayout build() {
            return new VAOLayout(elements.toArray(new LayoutElement[0]), size);
        }

        public PackedLayoutBuilder addFloat() {
             return this.addFloat(false);
        }

        public PackedLayoutBuilder addFloat(boolean normalized) {
            return this.addFloats(1, normalized);
        }

        public PackedLayoutBuilder addVec2() {
            return this.addVec2(false);
        }

        public PackedLayoutBuilder addVec2(boolean normalized) {
            return this.addFloats(2, normalized);
        }

        public PackedLayoutBuilder addVec3() {
            return this.addVec3(false);
        }

        public PackedLayoutBuilder addVec3(boolean normalized) {
            return this.addFloats(3, normalized);
        }

        public PackedLayoutBuilder addVec4() {
            return this.addVec4(false);
        }

        public PackedLayoutBuilder addVec4(boolean normalized) {
            return this.addFloats(4, normalized);
        }

        private PackedLayoutBuilder addFloats(int numFloats, boolean normalized) {
            this.elements.add(new FloatLayoutElement(elements.size(), 0, size, numFloats, normalized));
            this.size += numFloats * 4;
            return this;
        }

        public PackedLayoutBuilder addInt() {
            return this.addInts(1);
        }

        public PackedLayoutBuilder addIVec2() {
            return this.addInts(2);
        }

        public PackedLayoutBuilder addIVec3() {
            return this.addInts(3);
        }

        public PackedLayoutBuilder addIVec4() {
            return this.addInts(4);
        }

        private PackedLayoutBuilder addInts(int numInts) {
            this.elements.add(new IntLayoutElement(elements.size(), 0, size, numInts));
            this.size += numInts * 4;
            return this;
        }

        public PackedLayoutBuilder addDouble() {
            return this.addDoubles(1);
        }

        public PackedLayoutBuilder addDVec2() {
            return this.addDoubles(2);
        }

        public PackedLayoutBuilder addDVec3() {
            return this.addDoubles(3);
        }

        public PackedLayoutBuilder addDVec4() {
            return this.addDoubles(4);
        }

        private PackedLayoutBuilder addDoubles(int numDoubles) {
            this.elements.add(new DoubleLayoutElement(elements.size(), 0, size, numDoubles));
            this.size += numDoubles * 4;
            return this;
        }

        public PackedLayoutBuilder addByte() {
            return this.addBytes(1);
        }

        public PackedLayoutBuilder addBVec2() {
            return this.addBytes(2);
        }

        public PackedLayoutBuilder addBVec3() {
            return this.addBytes(3);
        }

        public PackedLayoutBuilder addBVec4() {
            return this.addBytes(4);
        }

        private PackedLayoutBuilder addBytes(int numBytes) {
            this.elements.add(new ByteLayoutElement(elements.size(), 0, size, numBytes));
            this.size += numBytes * 4;
            return this;
        }
    }

    public static class SingleBufferBuilder {
        private final List<LayoutElement> elements = new ArrayList<>();
        private final int stride;

        public SingleBufferBuilder(int stride) {
            this.stride = stride;
        }

        public VAOLayout build() {
            return new VAOLayout(elements.toArray(new LayoutElement[0]), stride);
        }

        public SingleBufferBuilder addFloats(int numFloats, int offset) {
            this.elements.add(new FloatLayoutElement(elements.size(), 0, offset, numFloats, false));
            return this;
        }

        public SingleBufferBuilder addInts(int numInts, int offset) {
            this.elements.add(new IntLayoutElement(elements.size(), 0, offset, numInts));
            return this;
        }

        public SingleBufferBuilder addDoubles(int numDoubles, int offset) {
            this.elements.add(new DoubleLayoutElement(elements.size(), 0, offset, numDoubles));
            return this;
        }

        public SingleBufferBuilder addBytes(int numBytes, int offset) {
            this.elements.add(new ByteLayoutElement(elements.size(), 0, offset, numBytes));
            return this;
        }
    }
}
