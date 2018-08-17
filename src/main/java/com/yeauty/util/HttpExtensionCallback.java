package com.yeauty.util;

import io.netty.handler.codec.http.HttpHeaders;

@FunctionalInterface
public interface HttpExtensionCallback {

    void response(String content, HttpHeaders httpHeaders, Integer status, Throwable cause);

}
