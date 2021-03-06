#### netty-time

springboot 整合netty编写时间服务器


这个例子与[上个例子(springboot 整合netty做心跳检测)](http://www.haoxiaoyong.cn:8080/articles/2018/10/19/1539918964664.html)最大的不同就是,服务端发送包含32位整数的消息，而不接收任何请求，并在发送消息后关闭连接。

因为我们将忽略任何接收到的数据，一旦建立连接就发送消息，这次我们不能使用channelRead()方法。 相反，我们应该重写channelActive()方法。
#### 项目依赖:
```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.21.Final</version>
</dependency>
```
#### 服务端
```java
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
         * 我们怎么知道写请求是否完成？这就像向返回的ChannelFuture添加ChannelFutureListener一样。
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

```
当然这也需要一个服务**启动引导类** 
和[上个例子(springboot 整合netty做心跳检测)](http://www.haoxiaoyong.cn:8080/articles/2018/10/19/1539918964664.html)基本是一样,更多详细说明已经在[上个例子(springboot 整合netty做心跳检测)](http://www.haoxiaoyong.cn:8080/articles/2018/10/19/1539918964664.html)中进行了说明,这个就不在阐述
```java
@Component
public class DiscardServer {
    @Value("${netty.server.port}")
    private int port;

    @PostConstruct
    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new TimeServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定并开始接受传入连接。
            ChannelFuture f = b.bind(port).sync();

            // 等到服务器套接字关闭。
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
```
  #### 客户端代码:  
```java
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
```
同样这里不做代码解释代码解释说明都放在了[上个例子(springboot 整合netty做心跳检测)](http://www.haoxiaoyong.cn:8080/articles/2018/10/19/1539918964664.html)

**下面代码接受服务端的消息并打印**

```java
public class TimeClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //Netty将从对等方发送的数据读入ByteBuf
        ByteBuf m = (ByteBuf) msg;
        try {
            long currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L;
            System.out.println("收到服务端发送的消息"+new Date(currentTimeMillis));
            ctx.close();
        } finally {
            m.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```
打印结果:
![在这里插入图片描述](https://img-blog.csdn.net/20181022095010573?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2hhb3hpYW95b25nMTAxNA==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
