package io.bicycle.epoll;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 3:41 PM
 */
public final class PollEvent {
    private final EventPoller source;
    private final String filename;
    private final Set<Type> types;
    private final Object data;

    public enum Type {
        EPOLLIN,
        EPOLLPRI,
        EPOLLOUT,
        EPOLLRDNORM,
        EPOLLRDBAND,
        EPOLLWRNORM,
        EPOLLWRBAND,
        EPOLLMSG,
        EPOLLERR,
        EPOLLHUP,
        EPOLLONESHOT,
        EPOLLET;

        static Set<Type> fromRawType(final int raw) {
            Set<Type> types = new HashSet<Type>();
            if((raw & EventPolling.EPOLLPRI) != 0) {
                types.add(EPOLLPRI);
            }
            if((raw & EventPolling.EPOLLIN) != 0) {
                types.add(EPOLLIN);
            }
            if((raw & EventPolling.EPOLLOUT) != 0) {
                types.add(EPOLLOUT);
            }
            if((raw & EventPolling.EPOLLRDNORM) != 0) {
                types.add(EPOLLRDNORM);
            }
            if((raw & EventPolling.EPOLLRDBAND) != 0) {
                types.add(EPOLLRDBAND);
            }
            if((raw & EventPolling.EPOLLWRNORM) != 0) {
                types.add(EPOLLWRNORM);
            }
            if((raw & EventPolling.EPOLLWRBAND) != 0) {
                types.add(EPOLLWRBAND);
            }
            if((raw & EventPolling.EPOLLMSG) != 0) {
                types.add(EPOLLMSG);
            }
            if((raw & EventPolling.EPOLLERR) != 0) {
                types.add(EPOLLERR);
            }
            if((raw & EventPolling.EPOLLHUP) != 0) {
                types.add(EPOLLHUP);
            }
            if((raw & EventPolling.EPOLLONESHOT) != 0) {
                types.add(EPOLLONESHOT);
            }
            if((raw & EventPolling.EPOLLET) != 0) {
                types.add(EPOLLET);
            }
            return types;
        }
    }


    PollEvent(EventPoller source, String filename, Set<Type> types, Object data) {
        this.source = source;
        this.filename = filename;
        this.types = types;
        this.data = data;
    }

    public EventPoller getSource() {
        return source;
    }

    public String getFilename() {
        return filename;
    }

    public Set<Type> getType() {
        return types;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("(EpollEvent. ");
        sb.append(source);
        sb.append(", ").append(filename);
        sb.append(", #{");

        int c = types.size() - 1;
        for (Type type : types) {
            sb.append(type);
            if(c > 0) {
                sb.append(", ");
            }
            --c;
        }

        sb.append("})");
        return sb.toString();
    }

}
