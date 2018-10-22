package com.netty.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Created by haoxy on 2018/10/18.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */

public class TimeServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 这次我们不能使用channelRead（）方法。相反，我们应该重写channelActive（）方法
     */

    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        /**
         * 要发送新消息，我们需要分配一个包含消息的新缓冲区。我们要写一个32位整数，
         * 因此我们需要一个容量至少为4个字节的ByteBuf。
         * 通过ChannelHandlerContext.alloc（）获取当前的ByteBufAllocator并分配一个新的缓冲区。
         */
        final ByteBuf time = ctx.alloc().buffer(4);
        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

        final ChannelFuture cf = ctx.writeAndFlush(time);
        /**
         * 我们怎么知道写请求是否完成？这就像向返回的ChannelFuture添加ChannelFutureListener一样简单。
         * 在这里，我们创建了一个新的匿名ChannelFutureListener，它在操作完成时关闭Channel
         */
        /*cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                assert cf == future;
                ctx.close();
            }
        });*/
        //为了简化代码也可以这么写
        cf.addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 当由于I / O错误或由于处理事件时抛出异常导致的处理程序实现而由Netty引发异常时，使用Throwable调用exceptionCaught（）事件处理程序方法。
     * 在大多数情况下，应记录捕获的异常并在此处关闭其关联的通道，尽管此方法的实现可能会有所不同，具体取决于您要处理特殊情况的操作。
     * 例如，您可能希望在关闭连接之前发送带有错误代码的响应消息。
     * @param ctx
     * @param cause
     * @throws Exception
     */

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
