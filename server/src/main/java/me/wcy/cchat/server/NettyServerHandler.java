package me.wcy.cchat.server;

import com.google.gson.Gson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import me.wcy.cchat.server.model.CMessage;
import me.wcy.cchat.server.model.LoginInfo;
import me.wcy.cchat.server.model.MsgType;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Channel失效，从Map中移除
        NettyChannelMap.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        Gson gson = new Gson();
        CMessage message = gson.fromJson(msg, CMessage.class);
        if (message.getType() == MsgType.PING) {
            System.out.println("received ping from " + message.getFrom());
            Channel channel = NettyChannelMap.get(message.getFrom());
            if (channel != null) {
                channel.writeAndFlush(message.toJson());
            }
        } else if (message.getType() == MsgType.LOGIN) {
            LoginInfo loginInfo = gson.fromJson(message.getContent(), LoginInfo.class);
            if (UserManager.get().verify(loginInfo)) {
                loginInfo.setCode(200);
                loginInfo.setMsg("success");
                message.setContent(loginInfo.toJson());
                ctx.channel().writeAndFlush(message.toJson());
                NettyChannelMap.add(loginInfo.getAccount(), ctx.channel());
                System.out.println(loginInfo.getAccount() + " login");
            } else {
                loginInfo.setCode(400);
                loginInfo.setMsg("用户名或密码错误");
                message.setContent(loginInfo.toJson());
                ctx.channel().writeAndFlush(message.toJson());
            }
        } else if (message.getType() == MsgType.TEXT) {
            Channel channel = NettyChannelMap.get(message.getTo());
            if (channel != null) {
                channel.isWritable();
                channel.writeAndFlush(message.toJson()).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        System.out.println("send msg to " + message.getTo() + " failed");
                    }
                });
            }
        }
        ReferenceCountUtil.release(msg);
    }
}