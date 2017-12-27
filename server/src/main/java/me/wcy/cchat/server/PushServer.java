package me.wcy.cchat.server;

public class PushServer {

    public static void main(String[] args) {
        NettyServerBootstrap serverBootstrap = new NettyServerBootstrap(8300);
        serverBootstrap.bind();
    }
}
