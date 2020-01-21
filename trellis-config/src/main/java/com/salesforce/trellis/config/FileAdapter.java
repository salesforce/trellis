/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config;

import com.salesforce.trellis.config.impl.PathFileAdapter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Handle on a file somewhere.  Usually it's just a handle file on local disk but it could be remote or a mock.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface FileAdapter {

    /**
     * @return an adapter instance for the file at the given path.
     */
    static FileAdapter forPath(final Path p) {
        return PathFileAdapter.create(p);
    }

    /**
     * @return human-readable location information about this file.  Probably the absolute path.  Used for logging.
     */
    String getLocation();

    /**
     * @return a reader on the file contents.  The file must exist.
     */
    Reader getReader() throws IOException;

    /**
     * @return a writer on the file contents.  The file must exist.
     */
    Writer getWriter() throws IOException;

    /**
     * @return an adapter to the file at the given path relative to this file's path.
     */
    FileAdapter getRelativeFile(Path relativePath);

    /**
     * @return true if the file currently exists.
     */
    boolean exists();

    /**
     * Execute the given callable while holding non-exclusive file system- and thread-level locks for reading the
     * underlying file resource. The calling thread will block until the locks are available.
     * <p/>
     * In a parallel maven build, it's possible that multiple plugin instances in separate classloaders
     * to be simultaneously be touching the rules files, so it's a good idea to read files here.
     *
     * @return the value returned by the provided Callable.
     * @throws NoSuchFileException if the file does not exist.
     * @throws Exception           thrown by the provided Callable.
     */
    <T> T executeExclusiveRead(Callable<T> callable) throws NoSuchFileException, Exception;


    /**
     * Execute the given callable while holding exclusive file system- and thread-level locks for reading or writing to
     * the underlying file resource. The calling thread will block until the locks are available.
     * <p/>
     * If the file does not exist when this method is called, an empty file will be immediately created before the
     * callable is called (so that we have something to lock at the file system level).
     * <p/>
     * In a parallel maven build, it's possible that multiple plugin instances in separate classloaders
     * to be simultaneously be touching the rules files, so it's a good idea to write files here.
     *
     * @return the value returned by the provided Callable.
     * @throws NoSuchFileException if the directory containing the file does not exist.
     * @throws Exception           thrown by the provided Callable.
     */
    <T> T executeExclusiveWrite(Callable<T> callable) throws NoSuchFileException, Exception;
}
