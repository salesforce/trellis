package com.salesforce.trellis.config.impl;

import com.google.common.base.Throwables;
import com.salesforce.trellis.config.ParserListener;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
public class MockParserListener implements ParserListener {

    private boolean failFast = true;
    private final List<ParserEvent> events = new ArrayList();

    public List<ParserEvent> getEvents() {
        return events;
    }

    public MockParserListener() {
    }

    public MockParserListener(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public void notify(ParserEvent event) {
        if (event.getType() == ParserEventType.WARNING.ERROR && failFast) {
            if (event.getCause() != null) {
                Throwables.propagate(new Exception(
                    "error at " + event.getFileLocation() + ":" + event.getLineNumber() + ":" + event.getColumnNumber()
                        + event.getCause(), event.getCause()));
            } else {
                throw new RuntimeException(
                    event.getMessage() + " " + event.getFileLocation() + ":" + event.getLineNumber() + ":" + event
                        .getColumnNumber());
            }
        }
        events.add(requireNonNull(event));
    }

}
