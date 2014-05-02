package io.bicycle;


import io.bicycle.epoll.EventPolling;
import io.bicycle.epoll.EventPoller;
import io.bicycle.epoll.PollEvent;

import java.io.*;
import java.util.List;


/**
 * Unit test for simple App.
 */
public class AppTest {

    boolean thread_running = true;

    public static void main(String[] args) {
        try {
            new AppTest().runIt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runIt() throws Exception {

        System.out.println("Setting up gpio pin 18...");
        writeTo("/sys/class/gpio/export", "18");
        writeTo("/sys/class/gpio/gpio18/direction", "in");
        writeTo("/sys/class/gpio/gpio18/edge", "both");

        System.out.println("Opening value file...");
        final RandomAccessFile f = new RandomAccessFile("/sys/class/gpio/gpio18/value", "rw");

        final EventPoller poller = EventPolling.create();
        poller.addFile(f, EventPolling.EPOLLIN | EventPolling.EPOLLET | EventPolling.EPOLLPRI);

        System.out.println("Added epoll");

        printFile(f);

        new Thread(new Runnable() {

            @Override
            public void run() {
                while (thread_running) {
                    List<PollEvent> events = poller.poll(-1);
                    for (PollEvent event : events) {
                        System.out.println(event);
                        printFile(event.getFile());
                    }
                }
            }
        }).start();

        Thread.sleep(10000);
        thread_running = false;

        // Wait for cleanup
        Thread.sleep(10);

        poller.close();
        System.out.println("Closed poller");
        safeClose(f);
        writeTo("/sys/class/gpio/unexport", "18");

        System.exit(0);
    }

    private static void printFile(RandomAccessFile f) {
        if(f == null) {
            System.out.println("No file given.");
            return;
        }

        try {
            f.seek(0);
            System.out.println("value: " + (char) f.read());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void writeTo(String filename, String content) {
        PrintStream printStream = null;
        try {
            printStream = new PrintStream(new FileOutputStream(filename));
            printStream.print(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            safeClose(printStream);
        }
    }

    private static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
