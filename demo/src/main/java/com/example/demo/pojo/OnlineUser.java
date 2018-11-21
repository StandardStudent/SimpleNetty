package com.example.demo.pojo;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;

/**
 * 在线用户表
 */
public class OnlineUser {
    private static HashMap<String,ChannelHandlerContext> onlineUser = new HashMap<>();
    public static void put(String uid,ChannelHandlerContext ctx){
        onlineUser.put(uid,ctx);
    }
    public static void remove(String uid){
        onlineUser.remove(uid);
    }
    public static ChannelHandlerContext get(String uid){
        return onlineUser.get(uid);
    }
}
