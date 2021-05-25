package me.martiii.p2pbridgeserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Scanner;

public class P2PBridgeServer {
    public static void main(String[] args) throws Exception {
        new P2PBridgeServer(args);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Channel serverChannel;
    private Channel controlServerChannel;

    private SocketAddress add1, add2;
    private Channel ch1, ch2;

    private Channel ctrCh;

    public P2PBridgeServer(String[] args) throws Exception {
        new Thread(() -> {
            boolean close = false;
            Scanner scanner = new Scanner(System.in);
            while (!close) {
                String line = scanner.nextLine();
                switch (line) {
                    case "close":
                        if (serverChannel != null) {
                            logger.info("Shutting down netty servers...");
                            serverChannel.close();
                            controlServerChannel.close();
                        }
                        close = true;
                        break;
                    case "device1":
                        logger.info("device1 " + add1);
                        break;
                    case "device2":
                        logger.info("device2 " + add2);
                        break;
                    case "disconnect1":
                        if (add1 != null) {
                            ch1.close();
                        } else {
                            logger.info("No device connected as device 1");
                        }
                        break;
                    case "disconnect2":
                        if (add2 != null) {
                            ch2.close();
                        } else {
                            logger.info("No device connected as device 2");
                        }
                        break;
                    case "": break;     //Ignore empty command
                    default:
                        logger.info("Unknown command. Available commands: device1, device2, disconnect1, disconnect2, close");
                }
            }
        }, "Console Commands Thread").start();

        int serverPort;
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        } else {
            serverPort = 7635;    //Default server port
        }

        int controlPort;
        if (args.length > 1) {
            controlPort = Integer.parseInt(args[1]);
        } else {
            controlPort = 7636;    //Default control port
        }

        SslContext sslCtx;
        if (args.length > 2 && args[2].equals("true")) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
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
                                    if (cause instanceof IOException) {
                                        logger.info(cause.getMessage());
                                    } else {
                                        cause.printStackTrace();
                                    }
                                    Channel ch = ctx.channel();
                                    clientDisconnected(ch);
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture serverBindFuture = serverBootstrap.bind(serverPort);
            logger.info("Starting netty server on port " + serverPort + (sslCtx != null ? " (ssl)" : ""));
            serverChannel = serverBindFuture.sync().channel();
            logger.info("Netty server successfully started and bound to " + serverChannel.localAddress());


            ServerBootstrap controlBootstrap = new ServerBootstrap();
            controlBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            p.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    ctrCh = ctx.channel();
                                    logger.info("Control Desktop App connected from " + ctrCh.remoteAddress());
                                    ctrCh.writeAndFlush("control server connected\n");
                                    if (add1 != null) {
                                        ctrCh.writeAndFlush("connected1 " + add1 + "\n");
                                    }
                                    if (add2 != null) {
                                        ctrCh.writeAndFlush("connected2 " + add2 + "\n");
                                    }
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) {
                                    ctrCh = null;
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    if (cause instanceof IOException) {
                                        logger.info(cause.getMessage() + " (control desktop)");
                                    } else {
                                        cause.printStackTrace();
                                    }
                                    ctx.close();
                                }
                            });
                        }
                    });
            
            ChannelFuture controlBindFuture = controlBootstrap.bind(controlPort);
            logger.info("Starting netty server (control desktop) on port " + controlPort + (sslCtx != null ? " (ssl)" : ""));
            controlServerChannel = controlBindFuture.sync().channel();
            logger.info("Netty server (control desktop) successfully started and bound to " + controlServerChannel.localAddress());

            serverChannel.closeFuture().sync();
            controlServerChannel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void clientConnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        if (add1 == null) {
            add1 = address;
            ch1 = channel;
            log("connected1 " + address);
        } else if (add2 == null){
            add2 = address;
            ch2 = channel;
            log("connected2 " + address);
        } else {
            log("connected? " + address);
        }
    }

    public void clientDisconnected(Channel channel) {
        SocketAddress address = channel.remoteAddress();
        if (address.equals(add1)) {
            add1 = null;
            log("disconnected1 " + address);
        } else if (address.equals(add2)) {
            add2 = null;
            log("disconnected2 " + address);
        } else {
            log("disconnected? " + address);
        }
    }

    private void clientMsgRead(Channel channel, ByteBuf msg) {
        SocketAddress address = channel.remoteAddress();
        String hex = ByteBufUtil.hexDump(msg);
        String ascii = new String(ByteBufUtil.getBytes(msg));
        if (address == add1) {
            if (add2 != null) {
                ch2.writeAndFlush(msg);
            }
            log("data1 " + hex + " " + ascii);
        } else if (address == add2) {
            if (add1 != null) {
                ch1.writeAndFlush(msg);
            }
            log("data2 " + hex + " " + ascii);
        } else {
            log("data? " + address + " " + hex + " " + ascii);
        }
    }

    private void log(String msg) {
        logger.info(msg);
        if (ctrCh != null) {
            ctrCh.writeAndFlush(msg + "\n");
        }
    }
}
