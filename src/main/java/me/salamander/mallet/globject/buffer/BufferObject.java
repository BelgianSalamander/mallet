package me.salamander.mallet.globject.buffer;

import me.salamander.mallet.MalletContext;
import me.salamander.mallet.type.BasicType;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.util.MathHelper;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Type;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL45.*;

public class BufferObject {
    private final int handle;
    private int size;

    private int usage;

    private int defaultBindingPoint = GL_ARRAY_BUFFER;

    public BufferObject() {
        this.handle = glGenBuffers();
    }

    public void setDefaultBindingPoint(int defaultBindingPoint) {
        this.defaultBindingPoint = defaultBindingPoint;
    }

    public void bind() {
        this.bind(defaultBindingPoint);
    }

    public void bind(int bindingPoint) {
        glBindBuffer(bindingPoint, handle);
    }

    public void bindBase(int index) {
        this.bindBase(defaultBindingPoint, index);
    }

    public void bindBase(int bindingPoint, int index) {
        glBindBufferBase(bindingPoint, index, handle);
    }

    public void allocate(int bytes, int usage) {
        this.allocate(defaultBindingPoint, bytes, usage);
    }

    public void allocate(int bindingPoint, int bytes, int usage) {
        this.size = bytes;
        this.usage = usage;
        glBufferData(bindingPoint, bytes, usage);
    }

    public void set(int defaultBindingPoint, int offset, ByteBuffer data) {
        this.set(defaultBindingPoint, offset, data, 0, data.remaining());
    }

    public void set(int offset, ByteBuffer data) {
        if (offset + data.remaining() > size) {
            throw new IllegalArgumentException("Offset + data.remaining() > size");
        }

        glBufferSubData(GL_ARRAY_BUFFER, offset, data);
    }

    public void set(int offset, ByteBuffer data, int start, int length) {
        this.set(defaultBindingPoint, offset, data, start, length);
    }

    public void set(int bindingPoint, int offset, ByteBuffer data, int start, int length) {
        if (offset + length > size) {
            throw new IllegalArgumentException("Offset + length > size");
        }

        long pointer = MemoryUtil.memAddress(data) + start;
        nglBufferSubData(bindingPoint, offset, length, pointer);
    }

    public ByteBuffer read(int offset, int length) {
        return this.read(defaultBindingPoint, offset, length);
    }

    public ByteBuffer read(int bindingPoint, int offset, int length) {
        if (offset + length > size) {
            throw new IllegalArgumentException("Offset + length > size");
        }

        ByteBuffer buffer = MemoryUtil.memAlloc(length);

        //nglGetBufferSubData(bindingPoint, offset, length, MemoryUtil.memAddress(buffer));
        glGetBufferSubData(bindingPoint, offset, buffer);

        return buffer;
    }

    //// Write methods
    //Int
    public void write(int offset, int data, MalletContext context) {
        this.write(defaultBindingPoint, offset, data, context);
    }

    public void write(int bindingPoint, int offset, int data, MalletContext context) {
        this.write(bindingPoint, offset, data, context.getType(Type.INT_TYPE));
    }

    public void write(int bindingPoint, int offset, int data, MalletType type) {
        if (offset + type.getSize() > size) {
            throw new IllegalArgumentException("Offset + type.getSize() > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize());
            type.makeWriter(BasicType.IntWriter.class).write(buffer, data);
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public void writeArray(int offset, int[] data, MalletContext context) {
        this.writeArray(defaultBindingPoint, offset, data, context);
    }

    public void writeArray(int bindingPoint, int offset, int[] data, MalletContext context) {
        this.writeArray(bindingPoint, offset, data, context.getType(Type.INT_TYPE));
    }

    public void writeArray(int bindingPoint, int offset, int[] data, MalletType type) {
        if (offset + type.getSize() * data.length > size) {
            throw new IllegalArgumentException("Offset + type.getSize() * data.length > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize() * data.length);
            BasicType.IntWriter writer = type.makeWriter(BasicType.IntWriter.class);
            for (int datum : data) {
                writer.write(buffer, datum);
            }
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    //Float
    public void write(int offset, float data, MalletContext context) {
        this.write(defaultBindingPoint, offset, data, context);
    }

    public void write(int bindingPoint, int offset, float data, MalletContext context) {
        this.write(bindingPoint, offset, data, context.getType(Type.FLOAT_TYPE));
    }

    public void write(int bindingPoint, int offset, float data, MalletType type) {
        if (offset + type.getSize() > size) {
            throw new IllegalArgumentException("Offset + type.getSize() > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize());
            type.makeWriter(BasicType.FloatWriter.class).write(buffer, data);
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public void writeArray(int offset, float[] data, MalletContext context) {
        this.writeArray(defaultBindingPoint, offset, data, context);
    }

    public void writeArray(int bindingPoint, int offset, float[] data, MalletContext context) {
        this.writeArray(bindingPoint, offset, data, context.getType(Type.FLOAT_TYPE));
    }

    public void writeArray(int bindingPoint, int offset, float[] data, MalletType type) {
        if (offset + type.getSize() * data.length > size) {
            throw new IllegalArgumentException("Offset + type.getSize() * data.length > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize() * data.length);
            BasicType.FloatWriter writer = type.makeWriter(BasicType.FloatWriter.class);
            for (float datum : data) {
                writer.write(buffer, datum);
            }
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    //Double
    public void write(int offset, double data, MalletContext context) {
        this.write(defaultBindingPoint, offset, data, context);
    }

    public void write(int bindingPoint, int offset, double data, MalletContext context) {
        this.write(bindingPoint, offset, data, context.getType(Type.DOUBLE_TYPE));
    }

    public void write(int bindingPoint, int offset, double data, MalletType type) {
        if (offset + type.getSize() > size) {
            throw new IllegalArgumentException("Offset + type.getSize() > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize());
            type.makeWriter(BasicType.DoubleWriter.class).write(buffer, data);
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public void writeArray(int offset, double[] data, MalletContext context) {
        this.writeArray(defaultBindingPoint, offset, data, context);
    }

    public void writeArray(int bindingPoint, int offset, double[] data, MalletContext context) {
        this.writeArray(bindingPoint, offset, data, context.getType(Type.DOUBLE_TYPE));
    }

    public void writeArray(int bindingPoint, int offset, double[] data, MalletType type) {
        if (offset + type.getSize() * data.length > size) {
            throw new IllegalArgumentException("Offset + type.getSize() * data.length > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize() * data.length);
            BasicType.DoubleWriter writer = type.makeWriter(BasicType.DoubleWriter.class);
            for (double datum : data) {
                writer.write(buffer, datum);
            }
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    //Boolean
    public void write(int offset, boolean data, MalletContext context) {
        this.write(defaultBindingPoint, offset, data, context);
    }

    public void write(int bindingPoint, int offset, boolean data, MalletContext context) {
        this.write(bindingPoint, offset, data, context.getType(Type.BOOLEAN_TYPE));
    }

    public void write(int bindingPoint, int offset, boolean data, MalletType type) {
        if (offset + type.getSize() > size) {
            throw new IllegalArgumentException("Offset + type.getSize() > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize());
            type.makeWriter(BasicType.BooleanWriter.class).write(buffer, data);
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public void writeArray(int offset, boolean[] data, MalletContext context) {
        this.writeArray(defaultBindingPoint, offset, data, context);
    }

    public void writeArray(int bindingPoint, int offset, boolean[] data, MalletContext context) {
        this.writeArray(bindingPoint, offset, data, context.getType(Type.BOOLEAN_TYPE));
    }

    public void writeArray(int bindingPoint, int offset, boolean[] data, MalletType type) {
        if (offset + type.getSize() * data.length > size) {
            throw new IllegalArgumentException("Offset + type.getSize() * data.length > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize() * data.length);
            BasicType.BooleanWriter writer = type.makeWriter(BasicType.BooleanWriter.class);
            for (boolean datum : data) {
                writer.write(buffer, datum);
            }
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    //Object
    public void write(int offset, Object data, MalletContext context) {
        this.write(defaultBindingPoint, offset, data, context);
    }

    public void write(int bindingPoint, int offset, Object data, MalletContext context) {
        this.write(bindingPoint, offset, data, context.getType(Type.getType(data.getClass())));
    }

    @SuppressWarnings("unchecked")
    public void write(int bindingPoint, int offset, Object data, MalletType type) {
        if (offset + type.getSize() > size) {
            throw new IllegalArgumentException("Offset + type.getSize() > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize());
            type.makeWriter(BiConsumer.class).accept(buffer, data);
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public void writeArray(int offset, Object[] data, MalletContext context) {
        this.writeArray(defaultBindingPoint, offset, data, context);
    }

    public void writeArray(int bindingPoint, int offset, Object[] data, MalletContext context) {
        this.writeArray(bindingPoint, offset, data, context.getType(Type.getType(data[0].getClass())));
    }

    @SuppressWarnings("unchecked")
    public void writeArray(int bindingPoint, int offset, Object[] data, MalletType type) {
        int roundedSize = MathHelper.align(type.getSize(), type.getAlignment());
        int neededSize = roundedSize * (data.length - 1) + type.getSize();

        if (offset + neededSize > size) {
            throw new IllegalArgumentException("Offset + neededSize > size");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(type.getSize() * data.length);
            BiConsumer<ByteBuffer, Object> writer = type.makeWriter(BiConsumer.class);
            for (Object datum : data) {
                writer.accept(buffer, datum);
            }
            buffer.flip();
            this.set(bindingPoint, offset, buffer);
        }
    }

    public int getSize() {
        return size;
    }

    public int getUsage() {
        return usage;
    }

    public int getDefaultBindingPoint() {
        return defaultBindingPoint;
    }

    public void release() {
        glDeleteBuffers(handle);
    }
}
