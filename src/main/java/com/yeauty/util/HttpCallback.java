package com.yeauty.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

@FunctionalInterface
public interface HttpCallback {

    void response(ChannelHandlerContext ctx, FullHttpResponse response, Throwable cause);

}
