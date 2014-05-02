package io.bicycle.epoll;

import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 3:41 PM
 */
public final class EpollEvent {
    private final Epoller source;
    private final RandomAccessFile file;
    private final Set<Type> types;

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
            if((raw & Epoll.EPOLLPRI) != 0) {
                types.add(EPOLLPRI);
            }
            if((raw & Epoll.EPOLLIN) != 0) {
                types.add(EPOLLIN);
            }
            if((raw & Epoll.EPOLLOUT) != 0) {
                types.add(EPOLLOUT);
            }
            if((raw & Epoll.EPOLLRDNORM) != 0) {
                types.add(EPOLLRDNORM);
            }
            if((raw & Epoll.EPOLLRDBAND) != 0) {
                types.add(EPOLLRDBAND);
            }
            if((raw & Epoll.EPOLLWRNORM) != 0) {
                types.add(EPOLLWRNORM);
            }
            if((raw & Epoll.EPOLLWRBAND) != 0) {
                types.add(EPOLLWRBAND);
            }
            if((raw & Epoll.EPOLLMSG) != 0) {
                types.add(EPOLLMSG);
            }
            if((raw & Epoll.EPOLLERR) != 0) {
                types.add(EPOLLERR);
            }
            if((raw & Epoll.EPOLLHUP) != 0) {
                types.add(EPOLLHUP);
            }
            if((raw & Epoll.EPOLLONESHOT) != 0) {
                types.add(EPOLLONESHOT);
            }
            if((raw & Epoll.EPOLLET) != 0) {
                types.add(EPOLLET);
            }
            return types;
        }
    }


    EpollEvent(Epoller source, RandomAccessFile file, Set<Type> types) {
        this.source = source;
        this.file = file;
        this.types = types;
    }

    public Epoller getSource() {
        return source;
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public Set<Type> getType() {
        return types;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("(EpollEvent. ");
        sb.append(source);
        sb.append(", ").append(file);
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
