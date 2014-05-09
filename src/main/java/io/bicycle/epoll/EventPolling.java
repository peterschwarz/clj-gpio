package io.bicycle.epoll;

import com.sun.jna.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 11:59 AM
 */
public class EventPolling {

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

    public static EventPoller create() {
        return create(1);
    }

    public static EventPoller create(int maxEvents) {
        if(initializationError != null) throw new RuntimeException("Native library is unavailable!", initializationError);
        if (maxEvents < 1) throw new AssertionError("Need at least 1 event");
        
        final int epfd = EventPolling.epoll_create(1);

        return new EventPollerImpl(epfd, maxEvents);
    }


    private static final class FileFDTuple {
        final RandomAccessFile file;
        final int fd;
        final NativePollEvent event;
        private  Object data;

        private FileFDTuple(RandomAccessFile file, int fd, NativePollEvent event, Object data) {
            this.file = file;
            this.fd = fd;
            this.event = event;
            this.data = data;
        }
    }

    private static class EventPollerImpl implements EventPoller {

        private final int epfd;
        private final int maxEvents;

        private final NativePollEvent[] events;

        private List<FileFDTuple> fileFDTuples = new ArrayList<FileFDTuple>();

        EventPollerImpl(int epfd, int maxEvents) {
            this.epfd = epfd;
            this.maxEvents = maxEvents;

            events = (NativePollEvent[]) new NativePollEvent().toArray(maxEvents);
        }

        @Override
        public void addFile(RandomAccessFile file, int flags) {
            this.addFile(file, flags, null);
        }

        @Override
        public void addFile(RandomAccessFile file, int flags, Object data) {
            int fd = nativeFd(file);
            final NativePollEvent event = new NativePollEvent(flags, new NativePollEventData(fd));
            event.write();

            if (epoll_ctl(epfd, EventPolling.EPOLL_CTL_ADD, fd, event.getPointer()) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }

            fileFDTuples.add(new FileFDTuple(file, fd, event, data));
        }

        @Override
        public void modifyFile(RandomAccessFile file, int flags) {
            modifyFile(file, flags, null);
        }

        @Override
        public void modifyFile(RandomAccessFile file, int flags, Object data) {
            final FileFDTuple tuple = tupleFor(file);
            final NativePollEvent event = tuple.event;
            event.events = flags;
            tuple.data = data;
            event.write();

            if (epoll_ctl(epfd, EventPolling.EPOLL_CTL_MOD, tuple.fd, event.getPointer()) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }
        }

        @Override
        public void removeFile(RandomAccessFile file) {
            final FileFDTuple tuple = tupleFor(file);

            deregister(tuple);

            fileFDTuples.remove(tuple);
        }

        private void deregister(FileFDTuple tuple) {
            if (epoll_ctl(epfd, EventPolling.EPOLL_CTL_DEL, tuple.fd, tuple.event.getPointer()) == -1) {
                throw new RuntimeException("Unable to add to epoll set");
            }
        }

        @Override
        public List<PollEvent> poll(int timeout) {
            int n;
            if ((n = epoll_wait(this.epfd, this.events[0].getPointer(), this.maxEvents, timeout)) == -1) {
                throw new RuntimeException("Interrupted");
            } else if (n > 0) {
                List<PollEvent> events = new ArrayList<PollEvent>(n);

                for (int i = 0; i < n; i++) {
                    this.events[i].read();

                    // System.out.println(this.events[i]);
                    final FileFDTuple tuple = tupleFor(this.events[i].data.fd);
                    events.add(new PollEvent(this,
                            tuple != null ? tuple.file : null,
                            PollEvent.Type.fromRawType(this.events[0].events),
                            tuple != null ? tuple.data : null));
                }

                return Collections.unmodifiableList(events);
            } else {
                return Collections.emptyList();
            }
        }


        public void close() {

            for (FileFDTuple tuple : fileFDTuples) {
                deregister(tuple);
            }

            if (EventPolling.close(this.epfd) == -1) {
                System.out.println("Unable to close epoller.");
            }
        }

        private FileFDTuple tupleFor(int fd) {
            for (FileFDTuple pair : fileFDTuples) {
                if (pair.fd == fd) {
                    return pair;
                }
            }
            return null;
        }

        private FileFDTuple tupleFor(RandomAccessFile file) {
            for (FileFDTuple pair : fileFDTuples) {
                if (pair.file.equals(file)) {
                    return pair;
                }
            }

            throw new RuntimeException("File not already associated with this epoller.");
        }

        private int nativeFd(RandomAccessFile file) {
            final int fd;
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


    private static final int EPOLL_CTL_ADD = 1;
    private static final int EPOLL_CTL_DEL = 2;
    private static final int EPOLL_CTL_MOD = 3;


    //extern int epoll_create (int __size) __THROW;
    private static native int epoll_create(int size);


    //extern int epoll_ctl (int __epfd, int __op, int __fd, struct epoll_event *__event) __THROW;
    private static native int epoll_ctl(int epfd, int op, int fd, Pointer event);

    //extern int epoll_wait (int __epfd, struct epoll_event *__events, int __maxevents, int __timeout);
//    public static native int epoll_wait(int epfd, epoll_event.ByReference events, int maxevents, int timeout);
    private static native int epoll_wait(int epfd, Pointer events, int maxevents, int timeout);

    private static native int close(int fd);

    private static UnsatisfiedLinkError initializationError = null;

    static {
      try {
        Native.register(EventPolling.class, Platform.C_LIBRARY_NAME);
      } catch (java.lang.UnsatisfiedLinkError e) {
        initializationError = e;
      }
    }

}
