/* 
 * Copyright (C) 2013 The Java Tool project
 * Gelin Luo <greenlaw110(at)gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.greenlaw110.util;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IO utilities
 */
// Most of the code come from Play!Framework IO.java, under Apache License 2.0
public class IO {

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            //
        }
    }

    public static File child(File file, String fn) {
        return new File(file, fn);
    }

    public static List<File> children(File file) {
        return Arrays.asList(file.listFiles());
    }

    public static File parent(File file) {
        return file.getParentFile();
    }
    
    public static File tmpFile() {
        return tmpFile(S.random(3), null, null);
    }

    public static File tmpFile(String prefix, String suffix) {
        return tmpFile(prefix, suffix, null);
    }

    public static File tmpFile(String prefix, String suffix, File dir) {
        if (null == prefix) {
            prefix = S.random(3);
        }
        try {
            return File.createTempFile(prefix, suffix, dir);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static OutputStream os(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static InputStream is(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static InputStream is(byte[] ba) {
        return new ByteArrayInputStream(ba);
    }

    public static BufferedOutputStream buffered(OutputStream os) {
        if (os instanceof BufferedOutputStream) {
            return (BufferedOutputStream)os;
        } else {
            return new BufferedOutputStream(os);
        }
    }

    public static BufferedInputStream buffered(InputStream is) {
        if (is instanceof BufferedInputStream) {
            return (BufferedInputStream)is;
        } else {
            return new BufferedInputStream(is);
        }
    }

    public static void delete(File file) {
        delete(file, false);
    }
    
    public static void delete(File f, boolean deleteChildren) {
        if (null == f || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            String[] sa = f.list();
            if (null != sa && sa.length > 0) {
                if (!deleteChildren) {
                    return;
                } else {
                    for (File f0 : f.listFiles()) {
                        delete(f0, true);
                    }
                }
            }
        }
        if (!f.delete()) {
            f.deleteOnExit();
        }
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     * @param file The file te read
     * @return The binary data
     */
    public static byte[] readContent(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(new BufferedInputStream(new FileInputStream(file)), baos);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
        return baos.toByteArray();
    }

    public static byte[] readContent(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is, baos);
        return baos.toByteArray();
    }

    /**
     * Read file content to a String (always use utf-8)
     *
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Read file content to a String
     *
     * @param url The url resource to read
     * @return The String content
     */
    public static String readContentAsString(URL url, String encoding) {
        try {
            return readContentAsString(url.openStream());
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Read file content to a String (always use utf-8)
     *
     * @param url the url resource to read
     * @return The String content
     */
    public static String readContentAsString(URL url) {
        return readContentAsString(url, "utf-8");
    }

    /**
     * Read file content to a String
     *
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        try {
            return readContentAsString(new FileInputStream(file), encoding);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static String readContentAsString(InputStream is) {
        return readContentAsString(is, "utf-8");
    }

    public static String readContentAsString(InputStream is, String encoding) {
        try {
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                if (lineNo++ > 0) out.println();
                out.print(line);
            }
            return result.toString();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(is);
        }
    }


    public static List<String> readLines(File file) {
        return readLines(file, "utf-8");
    }


    public static List<String> readLines(File file, String encoding) {
        List<String> lines = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            lines = readLines(is, encoding);
        } catch (IOException ex) {
            throw E.ioException(ex);
        } finally {
            close(is);
        }
        return lines;
    }

    public static List<String> readLines(InputStream is, String encoding) {
        if (encoding == null) {
            return readLines(is);
        } else {
            InputStreamReader r = null;
            try {
                r = new InputStreamReader(is, encoding);
            } catch (UnsupportedEncodingException e) {
                throw E.encodingException(e);
            }
            return readLines(r);
        }
    }

    public static List<String> readLines(InputStream inputStream) {
        InputStreamReader r = new InputStreamReader(inputStream);
        return readLines(r);
    }

    public static List<String> readLines(Reader input) {
        BufferedReader reader = new BufferedReader(input);
        List<String> list = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                list.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw E.ioException(e);
        }
        return list;
    }

    /**
     * Write String content to a file (always use utf-8)
     *
     * @param content The content to write
     * @param file    The file to write
     */
    public static void writeContent(CharSequence content, File file) {
        writeContent(content, file, "utf-8");
    }

    /**
     * Write String content to a file (always use utf-8)
     *
     * @param content The content to write
     * @param file    The file to write
     */
    public static void writeContent(CharSequence content, File file, String encoding) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, encoding));
            printWriter.println(content);
            printWriter.flush();
            os.flush();
        } catch (IOException e) {
            throw E.unexpected(e);
        } finally {
            close(os);
        }
    }

    public static void writeContent(CharSequence content, Writer writer) {
        try {
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println(content);
            printWriter.flush();
            writer.flush();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(writer);
        }
    }

    /**
     * Copy content from input stream to output stream without closing the output stream
     * 
     * @param is
     * @param os
     */
    public static void append(InputStream is, OutputStream os) {
        copy(is, os, false);
    }
    
    public static void copy(InputStream is, OutputStream os) {
        copy(is, os, true);
    }

    /**
     * Copy an stream to another one.
     */
    public static void copy(InputStream is, OutputStream os, boolean closeOs) {
        try {
            int read;
            byte[] buffer = new byte[8096];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        } catch(IOException e) {
            throw E.ioException(e);
        } finally {
            close(is);
            if (closeOs) {
                close(os);
            }
        }
    }

    /**
     * Alias of {@link #copy(java.io.InputStream, java.io.OutputStream)}
     * @param is
     * @param os
     */
    public static void write(InputStream is, OutputStream os) {
        copy(is, os);
    }
    
    public static void write(InputStream is, File f) {
        try {
            copy(is, new BufferedOutputStream(new FileOutputStream(f)));
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Write binay data to a file
     * @param data The binary data to write
     * @param file The file to write
     */
    public static void write(byte[] data, File file) {
        try {
            write(new ByteArrayInputStream(data), new BufferedOutputStream(new FileOutputStream(file)));
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    // If target does not exist, it will be created.
    public static void copyDirectory(File source, File target) {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdir();
            }
            for (String child: source.list()) {
                copyDirectory(new File(source, child), new File(target, child));
            }
        } else {
            try {
                write(new FileInputStream(source),  new FileOutputStream(target));
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
    }
    
    public static final class f {
        public static <T> F.IFunc1<?, T> println() {
            return PRINTLN;
        }
        
        public static F.IFunc1 PRINTLN = print("", "\n", System.out);

        public static <T> F.IFunc1<?, T> print() {
            return PRINT;
        }
        
        public static F.IFunc1 PRINT = print("", "", System.out);
        
        public static <T> F.IFunc1<?, T> print(String prefix, String suffix) {
            return print(prefix, suffix, System.out);
        }
        
        public static <T> F.IFunc1<?, T> print(String prefix, String suffix, PrintStream ps) {
            return new F.Op4<T, String, String, PrintStream>() {
                @Override
                public void operate(T t, String prefix, String suffix, PrintStream ps) {
                    StringBuilder sb = new StringBuilder(prefix).append(t).append(suffix);
                    ps.print(sb);
                }
            }.curry(prefix, suffix, ps);
        }
    }
}