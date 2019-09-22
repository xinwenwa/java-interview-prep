package com.stripe.interview;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class TimeHashMap {
  Map<String, TreeMap<Long, String>> timeMap;

  public TimeHashMap() {
    timeMap = new HashMap<>();
  }

  public void put(String key, String value) {
    if (!timeMap.containsKey(key)) {
      timeMap.put(key, new TreeMap<>());
    }
    timeMap.get(key).put(System.currentTimeMillis(), value);
  }

  public String get(String key) {
    if (!timeMap.containsKey(key)) {
      return "";
    }

    Long time = timeMap.get(key).firstKey();
    return timeMap.get(key).get(time);
  }

  public void delete(String key) {
    // delete currentTime
    Long currentTime = System.currentTimeMillis();
    if (timeMap.containsKey(key) && timeMap.get(key).containsKey(currentTime)) {
      timeMap.get(key).remove(currentTime);
    }
  }

  public static void main(String[] args) throws Exception {

  }
}
