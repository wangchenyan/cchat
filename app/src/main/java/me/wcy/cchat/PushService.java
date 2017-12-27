package me.wcy.cchat;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import me.wcy.cchat.model.CMessage;
import me.wcy.cchat.model.Callback;
import me.wcy.cchat.model.LoginInfo;
import me.wcy.cchat.model.LoginStatus;
import me.wcy.cchat.model.MsgType;

/**
 * Created by hzwangchenyan on 2017/12/26.
 */
public class PushService extends Service {
    private static final String TAG = "PushService";
    private static final String HOST = "10.240.78.82";
    private static final int PORT = 8300;

    private SocketChannel socketChannel;
    private Callback<Void> loginCallback;
    private Callback<CMessage> receiveMsgCallback;
    private Handler handler;
    private LoginStatus status = LoginStatus.UNLOGIN;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        AppCache.setService(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setReceiveMsgCallback(Callback<CMessage> receiveMsgCallback) {
        this.receiveMsgCallback = receiveMsgCallback;
    }

    private void connect(@NonNull Callback<Void> callback) {
        if (status == LoginStatus.CONNECTING) {
            return;
        }

        updateStatus(LoginStatus.CONNECTING);
        NioEventLoopGroup group = new NioEventLoopGroup();
        new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, 30, 0));
                        pipeline.addLast(new ObjectEncoder());
                        pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                        pipeline.addLast(new ChannelHandle());
                    }
                })
                .connect(new InetSocketAddress(HOST, PORT))
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        socketChannel = (SocketChannel) future.channel();
                        callback.onEvent(200, "success", null);
                    } else {
                        Log.e(TAG, "connect failed");
                        close();
                        // 这里一定要关闭，不然一直重试会引发OOM
                        future.channel().close();
                        group.shutdownGracefully();
                        callback.onEvent(400, "connect failed", null);
                    }
                });
    }

    public void login(String account, String token, Callback<Void> callback) {
        if (status == LoginStatus.CONNECTING || status == LoginStatus.LOGINING) {
            return;
        }

        connect((code, msg, aVoid) -> {
            if (code == 200) {
                LoginInfo loginInfo = new LoginInfo();
                loginInfo.setAccount(account);
                loginInfo.setToken(token);
                CMessage loginMsg = new CMessage();
                loginMsg.setFrom(account);
                loginMsg.setType(MsgType.LOGIN);
                loginMsg.setContent(loginInfo.toJson());
                socketChannel.writeAndFlush(loginMsg.toJson())
                        .addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                loginCallback = callback;
                            } else {
                                close();
                                updateStatus(LoginStatus.UNLOGIN);
                                if (callback != null) {
                                    handler.post(() -> callback.onEvent(400, "failed", null));
                                }
                            }
                        });
            } else {
                close();
                updateStatus(LoginStatus.UNLOGIN);
                if (callback != null) {
                    handler.post(() -> callback.onEvent(400, "failed", null));
                }
            }
        });
    }

    public void sendMsg(CMessage message, Callback<Void> callback) {
        if (status != LoginStatus.LOGINED) {
            callback.onEvent(401, "unlogin", null);
            return;
        }

        socketChannel.writeAndFlush(message.toJson())
                .addListener((ChannelFutureListener) future -> {
                    if (callback == null) {
                        return;
                    }
                    if (future.isSuccess()) {
                        handler.post(() -> callback.onEvent(200, "success", null));
                    } else {
                        handler.post(() -> callback.onEvent(400, "failed", null));
                    }
                });
    }

    private void close() {
        if (socketChannel != null) {
            socketChannel.close();
            socketChannel = null;
        }
    }

    private class ChannelHandle extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            PushService.this.close();
            updateStatus(LoginStatus.UNLOGIN);
            retryLogin(3000);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.WRITER_IDLE) {
                    // 空闲了，发个心跳吧
                    CMessage message = new CMessage();
                    message.setFrom(AppCache.getMyInfo().getAccount());
                    message.setType(MsgType.PING);
                    ctx.writeAndFlush(message.toJson());
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            Gson gson = new Gson();
            CMessage message = gson.fromJson(msg, CMessage.class);
            if (message.getType() == MsgType.LOGIN) {
                LoginInfo loginInfo = gson.fromJson(message.getContent(), LoginInfo.class);
                if (loginInfo.getCode() == 200) {
                    updateStatus(LoginStatus.LOGINED);
                    AppCache.setMyInfo(loginInfo);
                    if (loginCallback != null) {
                        handler.post(() -> {
                            loginCallback.onEvent(200, "success", null);
                            loginCallback = null;
                        });
                    }
                } else {
                    close();
                    updateStatus(LoginStatus.UNLOGIN);
                    if (loginCallback != null) {
                        handler.post(() -> {
                            loginCallback.onEvent(loginInfo.getCode(), loginInfo.getMsg(), null);
                            loginCallback = null;
                        });
                    }
                }
            } else if (message.getType() == MsgType.PING) {
                Log.d(TAG, "receive ping from server");
            } else if (message.getType() == MsgType.TEXT) {
                Log.d(TAG, "receive text message " + message.getContent());
                if (receiveMsgCallback != null) {
                    handler.post(() -> receiveMsgCallback.onEvent(200, "success", message));
                }
            }

            ReferenceCountUtil.release(msg);
        }
    }

    private void retryLogin(long mills) {
        if (AppCache.getMyInfo() == null) {
            return;
        }
        handler.postDelayed(() -> login(AppCache.getMyInfo().getAccount(), AppCache.getMyInfo().getToken(), (code, msg, aVoid) -> {
            if (code != 200) {
                retryLogin(mills);
            }
        }), mills);
    }

    private void updateStatus(LoginStatus status) {
        if (this.status != status) {
            Log.d(TAG, "update status from " + this.status + " to " + status);
            this.status = status;
        }
    }
}
