package io.quarkiverse.logback.runtime.events;

import org.xml.sax.Locator;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class LocatorImpl implements Locator {

    public final String publicId;
    public final String systemId;
    public final int lineNumber;
    public final int columnNumber;

    @RecordableConstructor
    public LocatorImpl(String publicId, String systemId, int lineNumber, int columnNumber) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public LocatorImpl(Locator locator) {
        this.publicId = locator.getPublicId();
        this.systemId = locator.getSystemId();
        this.lineNumber = locator.getLineNumber();
        this.columnNumber = locator.getColumnNumber();
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }
}
