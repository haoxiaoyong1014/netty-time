package com.netty.client.heart;

import com.netty.client.handler.TimeClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by haoxy on 2018/10/18.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 * Netty中服务器和客户端之间最大和唯一的区别是使用了不同的Bootstrap和Channel实现
 */
@Component
public class TimeClient {

    @Value("${netty.server.port}")
    private int port;
    @Value("${netty.server.host}")
    private String host;

    @PostConstruct
    public void timeClient() throws InterruptedException {

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup).channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast(new TimeClientHandler());
                }
            });
            //启动客户端
            ChannelFuture f = bootstrap.connect(host, port).sync();
            //等到连接关闭
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
