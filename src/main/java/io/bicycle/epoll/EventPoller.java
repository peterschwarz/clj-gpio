package io.bicycle.epoll;

import java.io.RandomAccessFile;
import java.util.List;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 3:32 PM
 */
public interface EventPoller {

    void addFile(String filename, int flags);

    void addFile(String filename, int flags, Object data);

    void modifyFile(String filename, int flags);

    void modifyFile(String filename, int flags, Object data);

    List<PollEvent> poll(int timeout);

    void removeFile(String filename);

    void close();
}
