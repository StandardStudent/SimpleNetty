package com.example.demo.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.pojo.OnlineUser;
import com.example.demo.util.JSONUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WSHandler extends SimpleChannelInboundHandler<Object> {
    private static ChannelGroup clients = new DefaultChannelGroup("all", GlobalEventExecutor.INSTANCE);
    private static ChannelGroup screen = new DefaultChannelGroup("screen", GlobalEventExecutor.INSTANCE);
    private static ChannelGroup cti = new DefaultChannelGroup("cti", GlobalEventExecutor.INSTANCE);
    private static ChannelGroup user = new DefaultChannelGroup("user", GlobalEventExecutor.INSTANCE);
    private static ChannelGroup sensor = new DefaultChannelGroup("sensor", GlobalEventExecutor.INSTANCE);
    private Logger logger = Logger.getLogger("nettyLog");
    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if (o instanceof FullHttpRequest) {
            handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
        }
        if (o instanceof WebSocketFrame) {
            if (o instanceof TextWebSocketFrame) {
                //接收到的内容
                String content = ((TextWebSocketFrame) o).text();
                JSONUtil jsonUtil = new JSONUtil();
                boolean flag = jsonUtil.isjson(content);
                JSONObject jsonObject = new JSONObject();
                if (flag) {
                    jsonObject = JSON.parseObject(content);
                }
                dataHandle(jsonObject,channelHandlerContext,content);
            }
            if (o instanceof CloseWebSocketFrame) {
                handshaker.close(channelHandlerContext.channel(), ((CloseWebSocketFrame) o).retain());
            }
            if (o instanceof PingWebSocketFrame) {
                channelHandlerContext.channel().write(new PongWebSocketFrame(((PingWebSocketFrame) o).content().retain()));
            }
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

    /**
     * 处理客户端向服务端发起http握手请求的业务
     *
     * @param ctx
     * @param req
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {

        // 如果不是WebSocket握手请求消息，那么就返回 HTTP 400 BAD REQUEST 响应给客户端。
        if (!req.getDecoderResult().isSuccess()
                || !("websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        //如果是握手请求，那么就进行握手
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8001/ws", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            // 通过它构造握手响应消息返回给客户端，
            // 同时将WebSocket相关的编码和解码类动态添加到ChannelPipeline中，用于WebSocket消息的编解码，
            // 添加WebSocketEncoder和WebSocketDecoder之后，服务端就可以自动对WebSocket消息进行编解码了
            handshaker.handshake(ctx.channel(), req);
        }
    }

    /**
     * 服务端向客户端响应消息
     *
     * @param ctx
     * @param req
     * @param res
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req,
                                  DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
        System.out.println("客户端与服务端连接开启，客户端remoteAddress：" + ctx.channel().remoteAddress());
    }

    //客户端与服务端断开连接的时候调用
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx.channel());
        System.out.println("客户端与服务端连接关闭...");
    }

    //服务端接收客户端发送过来的数据结束之后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    public void login(JSONObject jsonObject,ChannelHandlerContext channelHandlerContext){
        //添加用户名称以及对应的通道
        //！！！！！！！！少自定义分组！！！！！！！！！！！！！！
        Channel currentChannel = channelHandlerContext.channel();
        OnlineUser.put(jsonObject.getString("from"), channelHandlerContext);
        if (jsonObject.containsKey("token")) {
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
                new LogRecord(Level.INFO, "用户"
                        + jsonObject.getString("from") + "在" + LocalDateTime.now() + "登录"));
        //clients.writeAndFlush(new TextWebSocketFrame("用户在"+LocalDateTime.now()+"登录"));
    }

    /**
     * 数据处理
     * @param jsonObject 传递的json
     * @param channelHandlerContext 通道处理上下文
     * @param content 接收到的具体内容文字
     */
    public void dataHandle(JSONObject jsonObject,ChannelHandlerContext channelHandlerContext,String content){
        //如果有type字段
        if (jsonObject.containsKey("type")) {
            //用户登录
            if (jsonObject.get("type").equals("logon")) {
                login(jsonObject,channelHandlerContext);
            }
            //用户登出
            else if (jsonObject.get("type").equals("logout")) {
                OnlineUser.remove(jsonObject.getString("from"));
                logger.log(new LogRecord(Level.INFO, "用户"
                        + jsonObject.getString("from") + "在" + LocalDateTime.now() + "下线"));
                channelHandlerContext.close();
            }
            //传感器消息（推送给用户）
            else if (jsonObject.get("type").equals("sensor")) {
                //拿到用户手机号
                String userTel = jsonObject.getString("to");
                Channel c = (Channel) OnlineUser.get(userTel);
                c.writeAndFlush(new TextWebSocketFrame(content));
            }
            //传感器给大屏发
            else if (jsonObject.get("type").equals("alert")) {
                screen.writeAndFlush(new TextWebSocketFrame(content));
                cti.writeAndFlush(new TextWebSocketFrame(content));
            } else if (jsonObject.get("type").equals("engineer")) {
                screen.writeAndFlush(new TextWebSocketFrame(content));
            } else if (jsonObject.get("type").equals("repair")) {
                screen.writeAndFlush(new TextWebSocketFrame(content));
                cti.writeAndFlush(new TextWebSocketFrame(content));
            } else if (jsonObject.containsKey("HeartBeat")) {
            }
        } else {
            clients.writeAndFlush(new TextWebSocketFrame("传输内容格式不对，传输内容为：" + content));
        }
    }
}
