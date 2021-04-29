package me.martiii.p2pbridgeserver;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.nio.charset.Charset;

public class MainTest {

    @Test
    public void mainTest() throws InterruptedException {
        SslContext sslCtx = null;
        //sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        Channel device1 = newClient("192.168.1.65", sslCtx, "device 1");
        System.out.println("Device 1 connected");
        Thread.sleep(1000);
        Channel device2 = newClient("192.168.1.65", sslCtx, "device 2");
        System.out.println("Device 2 connected");
        Thread.sleep(1000);
        device1.writeAndFlush("This is a test! from device 1");
        System.out.println("Message sent from device 1");
        Thread.sleep(1000);
        device2.writeAndFlush("This is a test! from device 2");
        System.out.println("Message sent from device 2");
        Thread.sleep(1000);
        device1.close();
        device2.close();
    }

    private Channel newClient(String host, SslContext sslCtx, String clientName) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, 7635));
                        }
                        p.addLast(new StringDecoder());
                        p.addLast(new StringEncoder());
                        p.addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                System.out.println(clientName + " received: " + msg);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                cause.printStackTrace();
                                ctx.close();
                            }
                        });
                    }
                });

        ChannelFuture f = b.connect(host, 7635).sync();
        return f.channel();
    }
}
