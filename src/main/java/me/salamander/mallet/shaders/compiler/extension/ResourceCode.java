package me.salamander.mallet.shaders.compiler.extension;

import me.salamander.mallet.shaders.compiler.ShaderCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class ResourceCode implements GLSLCode {
    private final String path;

    public ResourceCode(String path) {
        this.path = path;
    }

    @Override
    public void write(StringBuilder sb, ShaderCompiler compiler) {
        InputStream is = compiler.getClass().getClassLoader().getResourceAsStream(path);

        if (is == null) {
            throw new RuntimeException("Could not find resource: " + path);
        }

        try {
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            sb.append(code);
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource: " + path);
        }
    }

    @Override
    public Set<GLSLCode> dependencies() {
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceCode that = (ResourceCode) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
