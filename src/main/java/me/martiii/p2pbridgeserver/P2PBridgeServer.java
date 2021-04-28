package me.martiii.p2pbridgeserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;

public class P2PBridgeServer {
    public static void main(String[] args) throws Exception {
        new P2PBridgeServer();
    }

    private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

    public P2PBridgeServer() throws Exception{
        final SslContext sslCtx;
        if (System.getProperty("ssl") != null) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        int port = Integer.parseInt(System.getProperty("port", "7635"));

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    clientConnected(ctx.channel());
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) {
                                    clientDisconnected(ctx.channel());
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                    clientMsgRead(ctx.channel(), (ByteBuf) msg);
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) {
                                    ctx.flush();
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    });

            b.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private SocketAddress add1, add2;
    private Channel ch1, ch2;

    private void clientConnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        logger.info("Device connected: " + address);
        if (add1 == null) {
            add1 = address;
            ch1 = channel;
            logger.info("Assigned as device 1");
        } else if (add2 == null){
            add2 = address;
            ch2 = channel;
            logger.info("Assigned as device 2");
        }
    }

    public void clientDisconnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        if (address.equals(add1)) {
            add1 = null;
            logger.info("Device 1 disconnected");
        } else if (address.equals(add2)) {
            add2 = null;
            logger.info("Device 2 disconnected");
        }
    }

    private void clientMsgRead(Channel channel, ByteBuf msg) {
        SocketAddress address = channel.remoteAddress();
        if (add1 != null && add2 != null) {
            String hex = ByteBufUtil.hexDump(msg);
            if (address == add1) {
                logger.info("New message from device 1 to device 2: " + hex);
                ch2.write(msg);
            } else if (address == add2) {
                logger.info("New message from device 2 to device 1: " + hex);
                ch1.write(msg);
            }
        }
    }
}
