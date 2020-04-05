package com.leo.buildsrc;

import java.util.HashMap;

/**
 * Created by qian on 2020-04-04
 * Describe:
 */
public class TimeManager {
    private static HashMap<String, Long> map = new HashMap<>();

    public static void addStartTime(String method, long time) {
        map.put(method, time);
    }

    public static void addEndTime(String method, long time) {
        if (map.containsKey(method)) {
            System.out.println("method:" + method + "   " +
                    time + "-" + map.get(method) + "=" + (time - map.get(method)));
        } else {
            System.out.println("method no time start...");
        }
    }
}
