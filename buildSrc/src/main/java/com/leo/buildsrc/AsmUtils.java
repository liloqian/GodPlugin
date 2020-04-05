package com.leo.buildsrc;

/**
 * Created by qian on 2020-04-04
 * Describe:
 */
@SuppressWarnings("ThrowableNotThrown")
public class AsmUtils {
    public static void handleMethodTime(boolean hello) {
        System.out.println(hello);
        String  a = new String("hello");
        String methodName = new Throwable().getStackTrace()[1].getMethodName();
        long startTime = System.nanoTime();
        System.out.println(methodName + ":start:" + startTime);
        long endTime = System.nanoTime();
        System.out.println(methodName + ":end:" + endTime);
        System.out.println(methodName + ":all:" + (endTime - startTime));
        a.substring(0);
    }
}
