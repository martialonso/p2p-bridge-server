package me.martiii.p2pbridgeserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class P2PBridgeServer {
    public static void main(String[] args) throws Exception {
        new P2PBridgeServer();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
                    //.handler(new LoggingHandler(LogLevel.INFO))
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

            ChannelFuture bindFuture = b.bind(port);
            logger.info("Starting netty server on port " + port);
            Channel channel = bindFuture.sync().channel();
            logger.info("Netty server successfully started and bound to " + channel.localAddress());
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private SocketAddress add1, add2;
    private Channel ch1, ch2;

    private void clientConnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        if (add1 == null) {
            add1 = address;
            ch1 = channel;
            logger.info("Connected from " + address + ", assigned as device 1");
        } else if (add2 == null){
            add2 = address;
            ch2 = channel;
            logger.info("Connected from " + address + ", assigned as device 2");
        } else {
            logger.info("Connected from " + address + ", not assigned (2 devices already connected)");
        }
    }

    public void clientDisconnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        if (address.equals(add1)) {
            add1 = null;
            logger.info("Disconnected from " + address + ", device 1 free");
        } else if (address.equals(add2)) {
            add2 = null;
            logger.info("Disconnected from " + address + ", device 2 free");
        }
    }

    private void clientMsgRead(Channel channel, ByteBuf msg) {
        SocketAddress address = channel.remoteAddress();
        String hex = ByteBufUtil.hexDump(msg);
        if (add1 != null && add2 != null) {
            if (address == add1) {
                ch2.writeAndFlush(msg);
                logger.info("Device 1 -> Device 2: " + hex);
            } else if (address == add2) {
                ch1.writeAndFlush(msg);
                logger.info("Device 2 -> Device 1: " + hex);
            } else {
                logger.info("Message from unassigned device " + address + ": " + hex);
            }
        } else {
            logger.info("Message from " + address + " but no peer to send: " + hex);
        }
    }
}
