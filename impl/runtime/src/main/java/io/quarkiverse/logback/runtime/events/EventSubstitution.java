package io.quarkiverse.logback.runtime.events;

import java.lang.reflect.Constructor;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.joran.spi.ElementPath;
import io.quarkus.runtime.ObjectSubstitution;

public class EventSubstitution implements ObjectSubstitution<SaxEvent, EventSub> {

    static final Constructor<StartEvent> START;
    static final Constructor<BodyEvent> BODY;
    static final Constructor<EndEvent> END;

    static {
        try {
            START = StartEvent.class.getDeclaredConstructor(ElementPath.class, String.class, String.class, String.class,
                    Attributes.class, Locator.class);
            BODY = BodyEvent.class.getDeclaredConstructor(String.class, Locator.class);
            END = EndEvent.class.getDeclaredConstructor(String.class, String.class, String.class, Locator.class);
            START.setAccessible(true);
            BODY.setAccessible(true);
            END.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventSub serialize(SaxEvent obj) {
        if (obj instanceof StartEvent) {
            StartEvent s = (StartEvent) obj;
            return new StartSub(obj.namespaceURI, obj.localName, obj.qName, new LocatorImpl(obj.locator),
                    new AttributesImpl(s.attributes), s.elementPath.getCopyOfPartList());
        } else if (obj instanceof BodyEvent) {
            return new BodySub(obj.namespaceURI, obj.localName, obj.qName, new LocatorImpl(obj.locator),
                    ((BodyEvent) obj).getText());
        } else if (obj instanceof EndEvent) {
            return new EndSub(obj.namespaceURI, obj.localName, obj.qName, new LocatorImpl(obj.locator));
        }
        throw new RuntimeException("Unknown event type");
    }

    @Override
    public SaxEvent deserialize(EventSub obj) {
        try {
            if (obj instanceof StartSub) {
                StartSub s = (StartSub) obj;
                return START.newInstance(new ElementPath(s.partList), s.namespaceURI, s.localName, s.qName, s.attributes,
                        s.locator);
            } else if (obj instanceof BodySub) {
                return BODY.newInstance(((BodySub) obj).text, obj.locator);
            } else if (obj instanceof EndSub) {
                EndSub e = (EndSub) obj;
                return END.newInstance(e.namespaceURI, e.localName, e.qName, e.locator);
            }
            throw new RuntimeException("Unknown event type");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
