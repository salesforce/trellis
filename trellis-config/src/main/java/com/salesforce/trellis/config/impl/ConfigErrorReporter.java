package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.ParserListener;
import com.salesforce.trellis.config.impl.SourceLocatable.SourceLocation;
import com.salesforce.trellis.rules.builder.RuleBuildingException;

import static java.util.Objects.requireNonNull;

/**
 * Accepts error messages while processing a config and translates them into ParserListener events.
 *
 * @author pcal
 * @since 0.0.1
 */
class ConfigErrorReporter {

    // ===================================================================
    // Fields

    private final ParserListener listener;
    private boolean isFatalErrorEncountered;

    // ===================================================================
    // Constructors

    ConfigErrorReporter(final ParserListener listenerOrNull) {
        this.listener = listenerOrNull;
    }

    // ===================================================================
    // Package methods

    void error(final FileAdapter file, final String message) {
        error(file, message, null, null);
    }

    void error(final FileAdapter file, final Exception e) {
        isFatalErrorEncountered = true;
        this.listener
            .notify(new ParserEventImpl(ParserListener.ParserEventType.ERROR, e.getMessage(), file, null, null, e));
    }

    private void error(final FileAdapter file, final String message, final Integer lineNumber, final Integer offset) {
        if (this.listener == null) return;
        isFatalErrorEncountered = true;
        this.listener
            .notify(new ParserEventImpl(ParserListener.ParserEventType.ERROR, message, file, lineNumber, offset, null));
    }

    void error(final SourceLocation loc, final Exception e) {
        requireNonNull(loc);
        isFatalErrorEncountered = true;
        this.listener.notify(
            new ParserEventImpl(ParserListener.ParserEventType.ERROR, e.getMessage(), loc.getSourceFile(),
                loc.getLineNumber(), loc.getColumnNumber(), e));
    }

    void error(final SourceLocation loc, final String message) {
        requireNonNull(loc);
        isFatalErrorEncountered = true;
        this.listener.notify(
            new ParserEventImpl(ParserListener.ParserEventType.ERROR, message, loc.getSourceFile(), loc.getLineNumber(),
                loc.getColumnNumber(), null));
    }

    boolean isFatalErrorEncountered() {
        return this.isFatalErrorEncountered;
    }

    void error(RuleBuildingException e) {
        isFatalErrorEncountered = true;
        this.listener.notify(
            new ParserEventImpl(ParserListener.ParserEventType.ERROR, e.getMessage(), null, null, null, null));
    }

    static final class ParserEventImpl implements ParserListener.ParserEvent {

        private final ParserListener.ParserEventType type;
        private final String message;
        private final FileAdapter file;
        private final Integer lineNumberOrNull;
        private final Integer columnNumberOrNull;
        private final Throwable causeOrNull;

        private ParserEventImpl(final ParserListener.ParserEventType type,
                                final String message,
                                final FileAdapter file,
                                final Integer lineNumberOrNull,
                                final Integer offsetOrNull,
                                final Throwable causeOrNull) {
            this.type = requireNonNull(type);
            this.message = requireNonNull(message);
            this.file = file;
            this.lineNumberOrNull = lineNumberOrNull;
            this.columnNumberOrNull = offsetOrNull;
            this.causeOrNull = causeOrNull;
        }

        @Override
        public ParserListener.ParserEventType getType() {
            return this.type;
        }

        @Override
        public String getMessage() {
            return this.message;
        }

        @Override
        public String getFileLocation() {
            return this.file.getLocation();
        }

        @Override
        public Integer getLineNumber() {
            return this.lineNumberOrNull;
        }

        @Override
        public Integer getColumnNumber() {
            return this.columnNumberOrNull;
        }

        @Override
        public Throwable getCause() {
            return this.causeOrNull;
        }
    }
}
