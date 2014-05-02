package io.bicycle.epoll;

import java.io.RandomAccessFile;
import java.util.List;

/**
 * User: pschwarz
 * Date: 5/1/14
 * Time: 3:32 PM
 */
public interface EventPoller {

    void addFile(RandomAccessFile file, int flags);

    void modifyFile(RandomAccessFile file, int flags);

    List<PollEvent> poll(int timeout);

    void removeFile(RandomAccessFile file);

    void close();
}
