package top.lukeewin.subtitle;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

/**
 * @author Luke Ewin
 * @date 2024/6/17 0:45
 * @blog blog.lukeewin.top
 */
public class WebsocketClient {
    public void init() {
        try {
            URI uri = new URI("wss://localhost:10095");
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            final String host = uri.getHost() == null ? "localhost" : uri.getHost();
            final int port = uri.getPort();

            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return;
            }

            final boolean ssl = "wss".equalsIgnoreCase(scheme);
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } else {
                sslCtx = null;
            }

            EventLoopGroup group = new NioEventLoopGroup();
            try {
                final WebSocketClientHandler handler =
                        new WebSocketClientHandler(
                                WebSocketClientHandshakerFactory.newHandshaker(
                                        uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpClientCodec(),
                                        new HttpObjectAggregator(8192),
                                        new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 65536),
                                        handler);
                            }
                        });

                Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
                handler.handshakeFuture().sync();

                // Send a message through the WebSocket connection
                // handler.send("Hello, Server!");

                ch.closeFuture().sync();
            } finally {
                group.shutdownGracefully();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
