package me.wcy.cchat.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

public class NettyChannelMap {
    private static Map<String, Channel> map = new ConcurrentHashMap<>();

    public static void add(String account, Channel channel) {
        map.put(account, channel);
    }

    public static Channel get(String clientId) {
        return map.get(clientId);
    }

    public static void remove(Channel channel) {
        for (Map.Entry entry : map.entrySet()) {
            if (entry.getValue() == channel) {
                String account = (String) entry.getKey();
                map.remove(account);
                System.out.println(account + " leave");
                break;
            }
        }
    }
}