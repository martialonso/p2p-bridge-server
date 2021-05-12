package me.martiii.p2pbridgecontrol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import me.martiii.p2pbridgecontrol.screen.Screen;

public class P2PBridgeControl {
    public static void main(String[] args) throws Exception {
        new P2PBridgeControl(args);
    }

    public P2PBridgeControl(String[] args) throws Exception {
        Screen screen = new Screen();

        SslContext sslCtx;
        if (System.getProperty("ssl") != null) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        String host = args[0];
        int port;
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        } else {
            port = 7636;    //Default port
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            p.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    screen.log(msg);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    screen.log(cause.toString());
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture f = b.connect(host, port).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}