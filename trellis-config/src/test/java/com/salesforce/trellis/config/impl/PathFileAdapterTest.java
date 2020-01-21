/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.salesforce.trellis.config.impl.ConfigTestUtils.model2string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test of reading and writing files with PathFileAdapter.
 *
 * @author pcal
 * @since 0.0.1
 */
public class PathFileAdapterTest {

    @Test
    public void testOpenForRead() throws Exception {
        final Path dir = Files.createTempDirectory("PathFileAdapterTest.testOpenForRead");
        final Path noSuchFile = dir.resolve("NoSuchFile.txt");
        final PathFileAdapter adapter = new PathFileAdapter(noSuchFile);
        // open the non-existent file for read and ensure it blows up
        try {
            adapter.executeExclusiveRead(() -> null);
            fail("did not get expected exception reading " + noSuchFile);
        } catch (NoSuchFileException expected) {}
        // open for write.  this should create it
        adapter.executeExclusiveWrite(() -> null);
        assertTrue(noSuchFile.toFile().exists());
        adapter.executeExclusiveRead(() -> null);
        assertTrue(noSuchFile.toFile().exists());
    }

    @Test
    public void testOpenForWrite() throws Exception {
        final Path dir = Files.createTempDirectory("PathFileAdapterTest.testOpenForWrite");
        final Path noSuchFile = dir.resolve("NoSuchFile.txt");
        // delete the temp directory and make sure we blow up
        Files.delete(dir);
        final PathFileAdapter adapter = new PathFileAdapter(noSuchFile);
        try {
            adapter.executeExclusiveWrite(() -> null);
            fail("did not get expected exception");
        } catch (NoSuchFileException expected) {}
        // now create the directory and make sure we can open for write (even though the file doesn't exist)
        dir.toFile().mkdirs();
        adapter.executeExclusiveWrite(() -> null);
    }

    @Test
    public void testBadInputs() throws Exception {
        final PathFileAdapter adapter =
            new PathFileAdapter(Files.createTempDirectory("PathFileAdapterTest.testBadInputs"));
        try {
            adapter.getRelativeFile(Paths.get("/absolute/path/bad"));
            fail("did not get expected exception");
        } catch (IllegalArgumentException expected) {}
        assertNotEquals("not a path", adapter);
        assertNotEquals(adapter, "not a path");
    }

    @Test
    public <T> void testContention() throws Throwable {
        final int NUMBER_OF_TASK_PAIRS = 100;
        final int THREAD_COUNT = 8;
        final String EXPECTED_TEXT = "The map is not the territory";
        final Path targetFile = Files.createTempFile("PathFileAdapterTest.testContention", "txt");
        FileUtils.writeStringToFile(targetFile.toFile(), EXPECTED_TEXT, "UTF-8");

        // utility class to sanity check the number of times a task successfully completes
        class Counter {
            private int count = 0;

            void increment() { count++; }

            int getCount() { return count; }

            void reset() { count = 0; }
        }
        final Counter counter = new Counter();

        final Collection<Callable<Throwable>> callables;
        {
            // Create a list of callables that will each alternately write a fixed string to a file and then read the
            // file and assert that it has the correct contents.  If anything goes wrong, they return an exception;
            // otherwise they return null.
            callables = new ArrayList<>();
            for (int i = 0; i < NUMBER_OF_TASK_PAIRS; i++) {
                callables.add(() -> {
                    try {
                        final PathFileAdapter adapter = new PathFileAdapter(targetFile);
                        return adapter.executeExclusiveWrite(() -> {
                            try (Writer w = adapter.getWriter()) {
                                IOUtils.write(EXPECTED_TEXT, w);
                            }
                            counter.increment();
                            return null;
                        });
                    } catch (Throwable t) {
                        return t;
                    }
                });
                callables.add(() -> {
                    final PathFileAdapter adapter = new PathFileAdapter(targetFile);
                    return adapter.executeExclusiveWrite(() -> {
                        try {
                            final String contents = IOUtils.toString(adapter.getReader());
                            assertEquals(EXPECTED_TEXT, contents);
                            counter.increment();
                            return null;
                        } catch (Throwable t) {
                            return t;
                        }
                    });
                });
            }
        }
        //
        // Sanity test: run all of the callables serially and make sure none of them had an exception.
        //
        for (final Callable<Throwable> c : callables) {
            final Throwable t = c.call();
            if (t != null) throw t;
        }
        assertEquals(NUMBER_OF_TASK_PAIRS * 2, counter.getCount());
        counter.reset();
        //
        // Now spin up a thread pool and let it run all of the callables.  If the locking isn't working correctly,
        // those reads and writes will quickly step on each other.
        //
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Throwable>> futures = executor.invokeAll(callables);
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(10);
        }
        //
        // Verify that none of them did step on each other.
        //
        int index = 0;
        for (final Future<Throwable> f : futures) {
            if (f.get() != null) {
                throw new Exception("failure encountered at index " + index, f.get());
            }
            index++;
        }
        assertEquals(NUMBER_OF_TASK_PAIRS * 2, counter.getCount());
        assertEquals(NUMBER_OF_TASK_PAIRS * 2, futures.size());
        counter.reset();
    }
}
