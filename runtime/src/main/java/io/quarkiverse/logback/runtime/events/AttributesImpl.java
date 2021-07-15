package io.quarkiverse.logback.runtime.events;

import java.util.Objects;

import org.xml.sax.Attributes;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class AttributesImpl implements Attributes {

    public Attribute[] attributes;

    public AttributesImpl(Attributes at) {
        attributes = new Attribute[at.getLength()];
        for (int i = 0; i < at.getLength(); ++i) {
            attributes[i] = new Attribute(at.getLocalName(i), at.getValue(i), at.getType(i), at.getURI(i), at.getQName(i));
        }
    }

    public AttributesImpl() {

    }

    @Override
    public int getLength() {
        return attributes.length;
    }

    @Override
    public String getURI(int index) {
        return attributes[index].uri;
    }

    @Override
    public String getLocalName(int index) {
        return attributes[index].localName;
    }

    @Override
    public String getQName(int index) {
        return attributes[index].qName;
    }

    @Override
    public String getType(int index) {
        return attributes[index].type;
    }

    @Override
    public String getValue(int index) {
        return attributes[index].value;
    }

    @Override
    public int getIndex(String uri, String localName) {
        for (int i = 0; i < attributes.length; ++i) {
            Attribute at = attributes[i];
            if (Objects.equals(uri, at.uri) && Objects.equals(localName, at.localName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String qName) {
        for (int i = 0; i < attributes.length; ++i) {
            Attribute at = attributes[i];
            if (Objects.equals(qName, at.qName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getType(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        }
        return attributes[index].type;
    }

    @Override
    public String getType(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        }
        return attributes[index].type;
    }

    @Override
    public String getValue(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            return null;
        }
        return attributes[index].value;
    }

    @Override
    public String getValue(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            return null;
        }
        return attributes[index].value;
    }

    public static class Attribute {
        public String localName;
        public String value;
        public String type;
        public String uri;
        public String qName;

        @RecordableConstructor
        public Attribute(String localName, String value, String type, String uri, String qName) {
            this.localName = localName;
            this.value = value;
            this.type = type;
            this.uri = uri;
            this.qName = qName;
        }
    }
}
