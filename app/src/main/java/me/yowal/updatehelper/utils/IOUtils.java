package me.yowal.updatehelper.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Collectors;

public class IOUtils {
    public static final int CR = '\r';

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static final char DIR_SEPARATOR = File.separatorChar;

    public static final int EOF = -1;

    public static final int LF = '\n';

    public static long copyLarge(final InputStream inputStream, final OutputStream outputStream)
            throws IOException {
        return copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
    }

    public static int copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final long count = copyLarge(inputStream, outputStream);
        if (count > Integer.MAX_VALUE) {
            return EOF;
        }
        return (int) count;
    }

    public static long copy(final InputStream inputStream, final OutputStream outputStream, final int bufferSize)
            throws IOException {
        return copyLarge(inputStream, outputStream, byteArray(bufferSize));
    }

    public static byte[] byteArray(final int size) {
        return new byte[size];
    }

    public static long copyLarge(final InputStream inputStream, final OutputStream outputStream, final byte[] buffer) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(outputStream, "outputStream");
        long count = 0;
        int n;
        while (EOF != (n = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static String toString(final InputStream input) throws IOException {
        return new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));
    }


}
