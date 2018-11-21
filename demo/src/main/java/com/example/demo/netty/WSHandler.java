package com.example.demo.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.pojo.OnlineUser;
import com.example.demo.util.JSONUtil;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WSHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static ChannelGroup clients = new DefaultChannelGroup("all",GlobalEventExecutor.INSTANCE);
    private static ChannelGroup screen = new DefaultChannelGroup("screen",GlobalEventExecutor.INSTANCE);
    private static ChannelGroup cti = new DefaultChannelGroup("cti",GlobalEventExecutor.INSTANCE);
    private static ChannelGroup user = new DefaultChannelGroup("user",GlobalEventExecutor.INSTANCE);
    private static ChannelGroup sensor = new DefaultChannelGroup("sensor",GlobalEventExecutor.INSTANCE);
    private Logger logger = Logger.getLogger("nettyLog");
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        //接收到的内容
        String content = textWebSocketFrame.text();
        JSONUtil jsonUtil = new JSONUtil();
        boolean flag =  jsonUtil.isjson(content);
        JSONObject jsonObject = new JSONObject();
        if (flag){
             jsonObject = JSON.parseObject(content);
        }
        Channel currentChannel = channelHandlerContext.channel();
        //如果有type字段
        if (jsonObject.containsKey("type")){
            //用户登录
            if (jsonObject.get("type").equals("logon")){
                //添加用户名称以及对应的通道
                //！！！！！！！！少自定义分组！！！！！！！！！！！！！！
                OnlineUser.put(jsonObject.getString("from"),channelHandlerContext);
                if (jsonObject.containsKey("token")){
                    switch (jsonObject.getString("token")) {
                        case "cti":
                            cti.add(currentChannel);
                            //ctiList.add(currentChannel);
                            break;
                        case "screen":
                            screen.add(currentChannel);
                            //screenList.add(currentChannel);
                            break;
                        case "user":
                            user.add(currentChannel);
                            //userList.add(currentChannel);
                            break;
                            default:
                                sensor.add(currentChannel);
                                //sensorList.add(currentChannel);
                                break;
                    }
                }
                logger.log(
                        new LogRecord(Level.INFO,"用户"
                                +jsonObject.getString("from")+"在"+LocalDateTime.now()+"登录"));
                //clients.writeAndFlush(new TextWebSocketFrame("用户在"+LocalDateTime.now()+"登录"));
            }
            //用户登出
            else if(jsonObject.get("type").equals("logout")) {
                OnlineUser.remove(jsonObject.getString("from"));
                logger.log(new LogRecord(Level.INFO,"用户"
                        +jsonObject.getString("from")+"在"+LocalDateTime.now()+"下线"));
            }
            //传感器消息（推送给用户）
            else if(jsonObject.get("type").equals("sensor")){
                //拿到用户手机号
                String userTel = jsonObject.getString("to");
                Channel c = (Channel) OnlineUser.get(userTel);
                c.writeAndFlush(new TextWebSocketFrame(content));
            }
            //传感器给大屏发
            else if(jsonObject.get("type").equals("alert")){
                screen.writeAndFlush(new TextWebSocketFrame(content));
                cti.writeAndFlush(new TextWebSocketFrame(content));
            }
            else if(jsonObject.get("type").equals("engineer")){
                screen.writeAndFlush(new TextWebSocketFrame(content));
            }
            else if(jsonObject.get("type").equals("repair")){
                screen.writeAndFlush(new TextWebSocketFrame(content));
                cti.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else {
            clients.writeAndFlush(new TextWebSocketFrame("传输内容格式不对，传输内容为："+content));
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
        System.out.println("客户端与服务端连接开启，客户端remoteAddress：" + ctx.channel().remoteAddress());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx.channel());
        //OnlineUser.remove(Integer.parseInt(ctx.channel().id().toString()));
        System.out.println("客户端与服务端连接关闭...，客户端remoteAddress：" + ctx.channel().remoteAddress());
    }
}
