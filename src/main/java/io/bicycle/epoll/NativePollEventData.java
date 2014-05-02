package io.bicycle.epoll;

import com.sun.jna.Pointer;
import com.sun.jna.Union;

/**
* User: pschwarz
* Date: 5/2/14
* Time: 3:16 PM
*/ // Public due to JNA needs
/*
typedef union epoll_data {
   void    *ptr;
   int      fd;
   uint32_t u32;
   uint64_t u64;
} epoll_data_t;
*/
@SuppressWarnings("UnusedDeclaration")
public class NativePollEventData extends Union {
    public Pointer ptr;
    public int fd;
    public int u32;
    public long u64;

    public NativePollEventData() {
        super();
    }

    public NativePollEventData(int fd_or_u32) {
        super();
        this.u32 = this.fd = fd_or_u32;
        setType(Integer.TYPE);
    }

    public NativePollEventData(long u64) {
        super();
        this.u64 = u64;
        setType(Long.TYPE);
    }

    public NativePollEventData(Pointer ptr) {
        super();
        this.ptr = ptr;
        setType(Pointer.class);
    }

    public String toString() {
        return String.format("epoll_data_t: ptr: %s, fd: %s, u32: %s", this.ptr, this.fd, this.u32);
    }


    public static class ByReference extends NativePollEventData implements com.sun.jna.Structure.ByReference {

    }

    public static class ByValue extends NativePollEventData implements com.sun.jna.Structure.ByValue {

    }
}
