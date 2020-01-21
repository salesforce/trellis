package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.FileAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

/**
 * Standard FileAdapter implementation that exposes a file on disk.
 *
 * @author pcal
 * @since 0.0.1
 */
public final class PathFileAdapter implements FileAdapter {

    private final Path path;

    public static FileAdapter create(Path p) {
        return new PathFileAdapter(p);
    }

    PathFileAdapter(Path p) {
        this.path = requireNonNull(p).toAbsolutePath();
    }

    @Override
    public String getLocation() {
        return path.toString();
    }

    @Override
    public Reader getReader() throws IOException {
        return new FileReader(path.toFile());
    }

    @Override
    public Writer getWriter() throws IOException {
        final File f = path.toFile();
        return new FileWriter(path.toFile());
    }

    @Override
    public FileAdapter getRelativeFile(Path relativePath) {
        if (requireNonNull(relativePath).isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative: " + relativePath);
        }
        return new PathFileAdapter(path.getParent().resolve(relativePath));
    }

    @Override
    public boolean exists() {
        return this.path.toFile().exists();
    }

    @Override
    public <T> T executeExclusiveRead(final Callable<T> callable) throws Exception {
        return executeExclusive(callable, this.path, true, READ);
    }

    @Override
    public <T> T executeExclusiveWrite(final Callable<T> callable) throws Exception {
        return executeExclusive(callable, this.path, false, READ, WRITE, CREATE);
    }

    /**
     * Utility method for performing tasks with exclusive locks on files.
     * <p/>
     * There are two things going on here: os-level locking and thread-level locking.  The os-level locking is arguably
     * overkill; it's pretty unlikely that other processes are going to be messing with the rules files.
     * <p/>
     * The thread-level locking, however, is absolutely essential.  In a parallel build in a multi-module project,
     * maven can decide to create new instances of plugins in new classloaders, seemingly at random.  It's possible
     * that it's a bug in maven.  But because plugins aren't supposed to touch resources outside of the module being
     * built, it isn't supposed to be a problem.
     * <p/>
     * Trellis, however, needs to maintain the rules files that are shared by the reactor all modules.  And in the case
     * of updating whitelists, we need to ensure that access to the files is synchronized.  Ideally, we'd just keep a
     * map of ReadWriteLocks for each file path.  But unfortunately, because of the classloader behavior mentioned
     * above, there's not an obvious place for us to stash that map.
     * <p/>
     * So, instead, we opt to simply synchronize all file access on an arbitrary class in a shared classloader.  This
     * is
     * not ideal but practically speaking it's not a big deal.  We're not doing that much I/O, and in the normal
     * enforcement case, all of the I/O is happening once (per plugin instance) in a single thread to load up all of
     * the rules.  And for updating whitelists, there will may end up being a very small bit of contention, but this is
     * a maintenance task that is not part of a normal build (and really the perf hit just can't be that high in real
     * terms).
     */
    private <T> T executeExclusive(final Callable<T> callable,
                                   final Path path,
                                   final boolean shared,
                                   final OpenOption... soo) throws Exception {
        requireNonNull(callable);
        requireNonNull(path);
        // an arbitrarily-chosen object for us to lock on.  the only requrement is that it has to have come from a
        // parent of all plugin classloaders.  and ideally it shouldn't be something that other clients are likely to be
        // locking on.
        final Object javaLock = Files.class;
        synchronized (javaLock) {
            try (final FileChannel fileChannel = FileChannel.open(path, soo);
                final FileLock osLock = fileChannel.lock(0L, Long.MAX_VALUE, shared)) {
                return callable.call();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathFileAdapter)) return false;
        final PathFileAdapter that = (PathFileAdapter) o;
        return this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public String toString() {
        return this.path.toString();
    }

    /**
     * Return the path to the file.  Exposed only for unit tests.
     */
    Path getPath() {
        return this.path;
    }
}
