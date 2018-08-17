# netty-http-client-utils

### Requirement
- jdk version 1.8 or 1.8+

### Maven Dependencies
```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-codec-http</artifactId>
    <version>4.1.25.Final</version>
</dependency>
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-handler</artifactId>
    <version>4.1.25.Final</version>
</dependency>
```
### Demo
- get method

```java
	String url = "http://www.baidu.com";

    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Chrome/68.0.3440.106 Safari/537.36");

    String charset = "utf-8";

    NettyHttpClientUtils.doGet(url, headers, charset,
            (content, httpHeaders, status, cause) -> {
                System.out.println(content);
                System.out.println(httpHeaders);
                System.out.println(status);
            });
```


- post method (form)

```java
    String url = "http://www.baidu.com";

    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Chrome/68.0.3440.106 Safari/537.36");

    Map<String, String> param = new HashMap<>();
    headers.put("key", "value");

    String charset = "utf-8";

    NettyHttpClientUtils.doPostForm(url, headers, param, charset,
            (content, httpHeaders, status, cause) -> {
                System.out.println(content);
                System.out.println(httpHeaders);
                System.out.println(status);
            });
```

- post method (json)

```java
    String url = "http://www.baidu.com";

    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Chrome/68.0.3440.106 Safari/537.36");

    String json = "{\"key\":\"value\"}";

    String charset = "utf-8";

    NettyHttpClientUtils.doPostJson(url, headers, json, charset,
            (content, httpHeaders, status, cause) -> {
                System.out.println(content);
                System.out.println(httpHeaders);
                System.out.println(status);
            });
```
