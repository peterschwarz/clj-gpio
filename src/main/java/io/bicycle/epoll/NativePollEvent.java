package io.bicycle.epoll;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
* User: pschwarz
* Date: 5/2/14
* Time: 3:16 PM
*/ // Public due to JNA needs
/*
 struct epoll_event {
    uint32_t     events;
    epoll_data_t data;
 };
 */
public class NativePollEvent extends Structure {
    /// Epoll events
    public int events;
    /// User data variable
    public NativePollEventData data;

    public NativePollEvent() {
        super();
        events = 0;
        data = new NativePollEventData(0);
    }

    protected List getFieldOrder() {
        return Arrays.asList("events", "data");
    }

    @SuppressWarnings("UnusedDeclaration")
    public NativePollEvent(Pointer p) {
        super(p);
        read();
    }

    @SuppressWarnings("UnusedDeclaration")
    public NativePollEvent(int events, NativePollEventData data) {
        super();
        this.events = events;
        this.data = data;
    }


    public String toString() {
        return String.format("epoll_event: events: %s data: %s", this.events, this.data);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class ByReference extends NativePollEvent implements Structure.ByReference {

    }

    @SuppressWarnings("UnusedDeclaration")
    public static class ByValue extends NativePollEvent implements Structure.ByValue {

    }

}
