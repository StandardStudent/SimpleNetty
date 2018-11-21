package com.example.demo.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

@Component
public class WSServer {

    private static class SingletionWSServer{
        static final WSServer instance = new WSServer();
    }

    public static WSServer getInstance(){
        return SingletionWSServer.instance;
    }

    private EventLoopGroup bossLoopGroup;
    private EventLoopGroup workerLoopGroup;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture channelFuture;

    public WSServer(){
        bossLoopGroup = new NioEventLoopGroup();
        workerLoopGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossLoopGroup,workerLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitializer())
                .option(ChannelOption.SO_BACKLOG,1024)
                .childOption(ChannelOption.SO_KEEPALIVE,true);
    }
    public void start(){
        this.channelFuture = serverBootstrap.bind(8001);
        System.out.println("netty websocket server 启动完毕");
    }
}
