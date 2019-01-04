package org.fuin.esc.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.validation.constraints.NotNull;

import org.eclipse.yasson.internal.JsonBinding;
import org.fuin.utils4j.Utils4J;
import org.jboss.weld.exceptions.IllegalStateException;

/**
 * Wrapper that uses a {@link PushbackReader} in all cases. Allows to read content and push it back during JSON-B parsing operations.
 */
public final class EscJsonb implements Jsonb {

    /** Key used for the {@link JsonbConfig} map. */
    public static final String CONFIG_KEY = EscJsonb.class.getName();

    private static final int PUSHBACK_SIZE = 200;

    private static final Charset ENCODING = Charset.forName("UTF-8");

    private final Jsonb delegate;

    private final int pushbackSize;

    private final Charset encoding;

    private PushbackReader reader;

    /**
     * Constructor with delegate.
     * 
     * @param config
     *            Configuration used to create the delegate.
     */
    public EscJsonb(@NotNull final JsonbConfig config) {
        this(config, PUSHBACK_SIZE, ENCODING);
    }

    /**
     * Constructor all mandatory data
     * 
     * @param config
     *            Configuration used to create the delegate.
     * @param pushbackSize
     *            The size of the pushback buffer.
     * @param encoding
     *            Encoding to use when reading from an input stream.
     */
    public EscJsonb(@NotNull final JsonbConfig config, final int pushbackSize, @NotNull final Charset encoding) {
        super();
        Utils4J.checkNotNull("config", config);
        Utils4J.checkNotNull("encoding", encoding);
        this.delegate = JsonbBuilder.create(config);
        if (!(this.delegate instanceof JsonBinding)) {
            throw new IllegalStateException(
                    "Context is expected to be type '" + JsonBinding.class.getName() + "', but was: " + this.delegate.getClass().getName());
        }
        this.pushbackSize = pushbackSize;
        this.encoding = encoding;
        config.setProperty(EscJsonb.CONFIG_KEY, this);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public <T> T fromJson(String str, Class<T> type) throws JsonbException {
        reader = new PushbackReader(new StringReader(str), pushbackSize) {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int count = super.read(cbuf, off, len);
                if (count > -1) {
                    final String text = new String(cbuf, off, count);
                    System.out.println("(" + text.length() + ")'" + text + "'");
                }
                return count;
            }
        };
        return delegate.fromJson(reader, type);
    }

    @Override
    public <T> T fromJson(String str, Type runtimeType) throws JsonbException {
        reader = new PushbackReader(new StringReader(str), pushbackSize);
        return delegate.fromJson(reader, runtimeType);
    }

    @Override
    public <T> T fromJson(Reader reader, Class<T> type) throws JsonbException {
        reader = new PushbackReader(reader, pushbackSize);
        return delegate.fromJson(reader, type);
    }

    @Override
    public <T> T fromJson(Reader reader, Type runtimeType) throws JsonbException {
        reader = new PushbackReader(reader, pushbackSize);
        return delegate.fromJson(reader, runtimeType);
    }

    @Override
    public <T> T fromJson(InputStream stream, Class<T> type) throws JsonbException {
        reader = new PushbackReader(new InputStreamReader(stream, encoding), pushbackSize);
        return delegate.fromJson(reader, type);
    }

    @Override
    public <T> T fromJson(InputStream stream, Type runtimeType) throws JsonbException {
        reader = new PushbackReader(new InputStreamReader(stream, encoding), pushbackSize);
        return delegate.fromJson(reader, runtimeType);
    }

    @Override
    public String toJson(Object object) throws JsonbException {
        return delegate.toJson(object);
    }

    @Override
    public String toJson(Object object, Type runtimeType) throws JsonbException {
        return delegate.toJson(object, runtimeType);
    }

    @Override
    public void toJson(Object object, Writer writer) throws JsonbException {
        delegate.toJson(object, writer);
    }

    @Override
    public void toJson(Object object, Type runtimeType, Writer writer) throws JsonbException {
        delegate.toJson(object, runtimeType, writer);
    }

    @Override
    public void toJson(Object object, OutputStream stream) throws JsonbException {
        delegate.toJson(object, stream);
    }

    @Override
    public void toJson(Object object, Type runtimeType, OutputStream stream) throws JsonbException {
        delegate.toJson(object, runtimeType, stream);
    }

    /**
     * Tries to find the given string on the underlying reader.
     * 
     * @param toFind String to find.
     * 
     * @return {@code true} if the string was found on the next characters of the reader. 
     */
    public boolean peek(final String toFind) {
        if (reader == null) {
            throw new IllegalStateException("reader == null");
        }
        try {
            final char[] cbuf = new char[pushbackSize];
            final int count = reader.read(cbuf, 0, pushbackSize);
            if (count == -1) {
                System.out.println("PEEK: -1");
                return false;
            }
            final String str = new String(cbuf, 0, count);
            reader.unread(cbuf, 0, count);
            System.out.println("PEEK:" + str);            
            return str.contains(toFind);
        } catch (final IOException ex) {
            throw new RuntimeException("Error reading from pushback stream", ex);
        }
    }

}
