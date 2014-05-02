package io.bicycle.epoll;

import com.sun.jna.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 11:59 AM
 */
public class Epoll {

    public static final int EPOLLIN = 0x001;
    public static final int EPOLLPRI = 0x002;
    public static final int EPOLLOUT = 0x004;
    public static final int EPOLLRDNORM = 0x040;
    public static final int EPOLLRDBAND = 0x080;
    public static final int EPOLLWRNORM = 0x100;
    public static final int EPOLLWRBAND = 0x200;
    public static final int EPOLLMSG = 0x400;
    public static final int EPOLLERR = 0x008;
    public static final int EPOLLHUP = 0x010;
    public static final int EPOLLONESHOT = (1 << 30);
    public static final int EPOLLET = (1 << 31);

    public static Epoller create() {
        return create(1);
    }

    public static Epoller create(int maxEvents) {
        if (maxEvents < 1) throw new AssertionError("Need at least 1 event");
        final int epfd = Epoll.epoll_create(1);

        return new EpollerImpl(epfd, maxEvents);
    }


    private static final class FileFDPair {
        final RandomAccessFile file;
        final int fd;

        private FileFDPair(RandomAccessFile file, int fd) {
            this.file = file;
            this.fd = fd;
        }
    }

    private static class EpollerImpl implements Epoller {

        private final int epfd;
        private final int maxEvents;

        private epoll_event[] events;

        private List<FileFDPair> fileFDPairs = new ArrayList<FileFDPair>();

        EpollerImpl(int epfd, int maxEvents) {
            this.epfd = epfd;
            this.maxEvents = maxEvents;

            events = (epoll_event[]) new epoll_event().toArray(maxEvents);
        }

        @Override
        public void addFile(RandomAccessFile file, int flags) {

            int fd = nativeFd(file);

            final Epoll.epoll_event.ByReference event = new Epoll.epoll_event.ByReference();
            event.events = flags;
            event.data.fd = fd;

            if (epoll_ctl(epfd, Epoll.EPOLL_CTL_ADD, fd, event) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }

            fileFDPairs.add(new FileFDPair(file, fd));
        }

        @Override
        public void modifyFile(RandomAccessFile file, int flags) {

            int fd = pairFor(file).fd;

            final Epoll.epoll_event.ByReference event = new Epoll.epoll_event.ByReference();
            event.events = flags;
            event.data.fd = fd;

            if (epoll_ctl(epfd, Epoll.EPOLL_CTL_MOD, fd, event) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }
        }

        @Override
        public void removeFile(RandomAccessFile file) {
            final FileFDPair pair = pairFor(file);
            int fd = pair.fd;

            deregister(fd);

            fileFDPairs.remove(pair);
        }

        private void deregister(int fd) {
            final epoll_event.ByReference event = new epoll_event.ByReference();
            if (epoll_ctl(epfd, Epoll.EPOLL_CTL_DEL, fd, event) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }
        }


        @Override
        public List<EpollEvent> poll(int timeout) {
            int n;
            if ((n = epoll_wait(this.epfd, this.events[0].getPointer(), this.maxEvents, timeout)) == -1) {
                throw new RuntimeException("Interrupted");
            } else if (n > 0) {
                List<EpollEvent> events = new ArrayList<EpollEvent>(n);

                for (int i = 0; i < n; i++) {
                    this.events[i].read();

                    System.out.println(this.events[i]);
                    final FileFDPair pair = pairFor(this.events[i].data.fd);
                    events.add(new EpollEvent(this, pair != null ? pair.file : null, EpollEvent.Type.fromRawType(this.events[0].events)));
                }

                return Collections.unmodifiableList(events);
            } else {
                return Collections.emptyList();
            }
        }


        public void close() {

            for (FileFDPair pair : fileFDPairs) {
                deregister(pair.fd);
            }

            if (Epoll.close(this.epfd) == -1) {
                System.out.println("Unable to close epoller.");
            }
        }

        private FileFDPair pairFor(int fd) {
            for (FileFDPair pair : fileFDPairs) {
                if (pair.fd == fd) {
                    return pair;
                }
            }
            return null;
        }

        private FileFDPair pairFor(RandomAccessFile file) {
            for (FileFDPair pair : fileFDPairs) {
                if (pair.file.equals(file)) {
                    return pair;
                }
            }

            throw new RuntimeException("File not already associated with this epoller.");
        }

        private int nativeFd(RandomAccessFile file) {
            int fd = 0;
            try {
                fd = NativeFileUtils.getFileHandle(file.getFD());
            } catch (IOException e) {
                throw new RuntimeException("Unable to get native file descriptor", e);
            }
            if (fd == -1) {
                throw new RuntimeException("Unable to get native file descriptor");
            }
            return fd;
        }

    }


    // Public due to JNA needs
    /*
    typedef union epoll_data {
       void    *ptr;
       int      fd;
       uint32_t u32;
       uint64_t u64;
    } epoll_data_t;
    */
    public static class epoll_data_t extends Union {
        public Pointer ptr;
        public int fd;
        public int u32;
        public long u64;

        public epoll_data_t() {
            super();
        }

        public epoll_data_t(int fd_or_u32) {
            super();
            this.u32 = this.fd = fd_or_u32;
            setType(Integer.TYPE);
        }

        public epoll_data_t(long u64) {
            super();
            this.u64 = u64;
            setType(Long.TYPE);
        }

        public epoll_data_t(Pointer ptr) {
            super();
            this.ptr = ptr;
            setType(Pointer.class);
        }

        public String toString() {
            return String.format("epoll_data_t: ptr: %s, fd: %s, u32: %s", this.ptr, this.fd, this.u32);
        }


        public static class ByReference extends epoll_data_t implements com.sun.jna.Structure.ByReference {

        }

        public static class ByValue extends epoll_data_t implements com.sun.jna.Structure.ByValue {

        }
    }

    // Public due to JNA needs
    /*
     struct epoll_event {
        uint32_t     events;
        epoll_data_t data;
     };
     */
    public static class epoll_event extends Structure {
        /// Epoll events
        public int events;
        /// User data variable
        public epoll_data_t data;

        public epoll_event() {
            super();
            this.setAlignType(Structure.ALIGN_NONE);
            events = 0;
            data = new epoll_data_t();
        }

        protected List getFieldOrder() {
            return Arrays.asList("events", "data");
        }

        public epoll_event(Pointer p) {
            super(p, Structure.ALIGN_NONE);
            read();
        }

        public epoll_event(int events, epoll_data_t data) {
            super();
            this.setAlignType(Structure.ALIGN_NONE);
            this.events = events;
            this.data = data;
        }

        @Override
        public void read() {
            super.read();
            this.data.read();
        }

        public String toString() {
            return String.format("epoll_event: events: %s data: %s", this.events, this.data);
        }

        public static class ByReference extends epoll_event implements Structure.ByReference {

        }

        public static class ByValue extends epoll_event implements Structure.ByValue {

        }

    }


    private static final int EPOLL_CTL_ADD = 1;
    private static final int EPOLL_CTL_DEL = 2;
    private static final int EPOLL_CTL_MOD = 3;


    //extern int epoll_create (int __size) __THROW;
    private static native int epoll_create(int size);


    //extern int epoll_ctl (int __epfd, int __op, int __fd, struct epoll_event *__event) __THROW;
    private static native int epoll_ctl(int epfd, int op, int fd, epoll_event.ByReference event);

    //extern int epoll_wait (int __epfd, struct epoll_event *__events, int __maxevents, int __timeout);
//    public static native int epoll_wait(int epfd, epoll_event.ByReference events, int maxevents, int timeout);
    private static native int epoll_wait(int epfd, Pointer events, int maxevents, int timeout);

    private static native int close(int fd);

    static {
        Native.register(Epoll.class, Platform.C_LIBRARY_NAME);
    }

}
