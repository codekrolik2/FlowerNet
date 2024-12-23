/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.flower.securechat;

import com.flower.utils.PkiUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.TrustManagerFactory;

import static com.flower.socksserver.FlowerSslContextBuilder.TLS_CIPHERS;
import static com.flower.socksserver.FlowerSslContextBuilder.TLS_PROTOCOLS;

/**
 * Simple SSL chat server modified from TelnetServer.
 */
public final class SecureChatServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));
//    private static final TrustManagerFactory TRUST_MANAGER = PkiUtil.getTrustManagerForCertificateResource("oneone_cert.pem");
//    private static final TrustManagerFactory TRUST_MANAGER = PkiUtil.getTrustManagerForCertificateResource("my.pem");
//    private static final TrustManagerFactory TRUST_MANAGER = PkiUtil.getTrustManagerForCertificateResource("test.crt");
//    private static final TrustManagerFactory TRUST_MANAGER = PkiUtil.getTrustManagerForCertificateResource("ca.crt");
    private static final TrustManagerFactory TRUST_MANAGER = PkiUtil.getTrustManagerForCertificateResource("pkcs11_ca.crt");

    public static void main(String[] args) throws Exception {
//        KeyManagerFactory keyManager = PkiUtil.getKeyManagerFromResources("MY_REQ.crt", "MY_REQ.key", "");
//        KeyManagerFactory keyManager = PkiUtil.getKeyManagerFromResources("test.crt", "test.key", "");
//        KeyManagerFactory keyManager = PkiUtil.getKeyManagerFromPKCS11("/usr/lib/libeToken.so", "Qwerty123");

        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder
              .forServer(ssc.certificate(), ssc.privateKey())
//            .forServer(keyManager)
            .protocols(TLS_PROTOCOLS)
            .ciphers(TLS_CIPHERS)
            .trustManager(TRUST_MANAGER)
            .clientAuth(ClientAuth.REQUIRE)
            .build();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new SecureChatServerInitializer(sslCtx));

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
