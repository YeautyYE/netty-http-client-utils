package com.yeauty.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/8/17 15:40
 */
public abstract class NettyHttpClientUtils {

    private static SslContext sslCtx;

    private static Bootstrap bootstrap;

    private static NioEventLoopGroup group;

    private static String FORM_EQUAL_SIGN = "=";
    private static String FORM_AND_SIGN = "&";

    static {
        try {
//            sslCtx = SslContextBuilder.forClient().build();
            sslCtx = SslContextBuilder
                    .forClient()
                    .sslProvider(SslProvider.JDK)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            e.printStackTrace();
        }

        init();
    }

    private static void init() {
        init(0);
    }

    static void init(int nThreads) {
        if (group != null) {
            group.shutdownGracefully().syncUninterruptibly();
        }

        group = new NioEventLoopGroup(nThreads);
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(512 * 1024))
                                .addLast(new HttpContentDecompressor());
                    }
                });

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (group != null) {
                    group.shutdownGracefully().syncUninterruptibly();
                }
            }
        }));
    }

    public static void connect(String url, DefaultFullHttpRequest httpRequest, HttpCallback httpCallback) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (scheme == null || scheme.trim().equals("")) {
            httpCallback.response(null, null, new URISyntaxException(url, "scheme is empty"));
        }
        int port = uri.getPort();
        switch (scheme) {
            case "http":
                if (port == -1) {
                    port = 80;
                }
                break;
            case "https":
                if (port == -1) {
                    port = 443;
                }
                break;
            default:
                httpCallback.response(null, null, new URISyntaxException(url, "scheme is unknow"));
        }

        ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), port);
        Channel channel = channelFuture.channel();
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                ChannelPipeline pipeline = channel.pipeline();
                if ("https".equals(scheme)) {
                    pipeline.addFirst(sslCtx.newHandler(channel.alloc()));
                }
                pipeline.addLast(new ResponseHandler(httpCallback));
                channel.writeAndFlush(httpRequest);
            }
            if (future.isCancelled()) {
                httpCallback.response(null, null, new IOException("connect is cancelled"));
                channel.close();
            }
        });
    }

    public static void doGet(String url, Map<String, String> headers, String charsetName, HttpExtensionCallback httpExtensionCallback) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, url);
        setHeaders(headers, request);
        requestAndParseRespond(url, charsetName, httpExtensionCallback, request);
    }

    public static void doPost(String url, Map<String, String> headers, ByteBuf body, String charsetName, AsciiString contentType, HttpExtensionCallback httpExtensionCallback) {
        DefaultFullHttpRequest request;
        if (body != null) {
            request = new DefaultFullHttpRequest(HTTP_1_1, POST, url, body);
        } else {
            request = new DefaultFullHttpRequest(HTTP_1_1, POST, url);
        }
        HttpUtil.setContentLength(request, request.content().readableBytes());
        setHeaders(headers, request);
        request.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);

        requestAndParseRespond(url, charsetName, httpExtensionCallback, request);
    }

    public static void doPostForm(String url, Map<String, String> headers, Map<String, String> param, String charsetName, HttpExtensionCallback httpExtensionCallback) {
        if (param != null && param.size() > 0) {
            ByteBuf body = buildFormBody(param, charsetName);
            doPost(url, headers, body, charsetName, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, httpExtensionCallback);
        } else {
            doPost(url, headers, null, charsetName, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, httpExtensionCallback);
        }
    }

    public static void doPostJson(String url, Map<String, String> headers, String json, String charsetName, HttpExtensionCallback httpExtensionCallback) {
        if (json != null && !json.trim().equals("")) {
            byte[] bodyBytes = json.getBytes();
            ByteBuf body = PooledByteBufAllocator.DEFAULT.buffer(bodyBytes.length).writeBytes(bodyBytes);
            doPost(url, headers, body, charsetName, HttpHeaderValues.APPLICATION_JSON, httpExtensionCallback);
        } else {
            doPost(url, headers, null, charsetName, HttpHeaderValues.APPLICATION_JSON, httpExtensionCallback);
        }
    }


    private static void requestAndParseRespond(String url, String charsetName, HttpExtensionCallback httpExtensionCallback, DefaultFullHttpRequest request) {
        connect(url, request, (ctx, response, throwable) -> {
            if (throwable != null) {
                httpExtensionCallback.response(null, null, null, throwable);
            } else {
                Charset charset;
                if (charsetName != null && !charsetName.trim().equals("")) {
                    charset = Charset.forName(charsetName);
                } else {
                    charset = Charset.defaultCharset();
                }
                String content = response.content().toString(charset);
                HttpHeaders httpHeaders = response.headers();
                int status = response.status().code();
                httpExtensionCallback.response(content, httpHeaders, status, null);
            }
        });
    }

    private static ByteBuf buildFormBody(Map<String, String> param, String charsetName) {
        final String charsetNameFin;
        if (charsetName == null || charsetName.trim().equals("")) {
            charsetNameFin = "utf-8";
        } else {
            charsetNameFin = charsetName;
        }
        StringBuffer sb = new StringBuffer();

        param.forEach((key, value) -> {
            try {
                sb.append(URLEncoder.encode(key, charsetNameFin)).append(FORM_EQUAL_SIGN).append(URLEncoder.encode(value, charsetNameFin)).append(FORM_AND_SIGN);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        String body = sb.substring(0, sb.length() - 1);
        byte[] bodyBytes = body.getBytes();
        return PooledByteBufAllocator.DEFAULT.buffer(bodyBytes.length).writeBytes(bodyBytes);
    }

    private static void setHeaders(Map<String, String> headers, DefaultFullHttpRequest request) {
        if (headers != null && headers.size() > 0) {
            headers.forEach((key, value) -> request.headers().set(key, value));
        }
    }
}

class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private HttpCallback httpCallback;

    public ResponseHandler(HttpCallback httpCallback) {
        this.httpCallback = httpCallback;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        if (ctx.channel().isActive()) {
            try {
                httpCallback.response(ctx, msg, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
        httpCallback.response(ctx, null, throwable);
        ctx.close();
    }
}

