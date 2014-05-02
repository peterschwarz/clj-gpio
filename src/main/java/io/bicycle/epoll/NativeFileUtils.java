package io.bicycle.epoll;

import java.io.*;
import java.lang.reflect.Field;

public final class NativeFileUtils {
 
    private static Field __fd;
    static {
        try {
            __fd = FileDescriptor.class.getDeclaredField("fd");
            __fd.setAccessible(true);
        } catch (Exception ex) {
            __fd = null;
        }   
    }

    public static int getFileHandle(FileDescriptor fd) {
        try {
            return __fd.getInt(fd);
        } catch (IllegalAccessException e) {
            return -1;
        }
    }

    public static int getOutputFileHandle(File f) {

        try {
            return getFileHandle(new FileOutputStream(f).getFD());
        } catch (IOException e) {
            return -1;
        }
    }

    public static int getInputFileHandle(File f) {
        try {
            return getFileHandle(new FileInputStream(f).getFD());
        } catch (IOException e) {
            return -1;
        }
    }

}