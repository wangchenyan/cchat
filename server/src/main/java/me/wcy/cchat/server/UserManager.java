package me.wcy.cchat.server;

import java.util.HashSet;
import java.util.Set;

import me.wcy.cchat.server.model.LoginInfo;

/**
 * Created by hzwangchenyan on 2017/12/27.
 */
public class UserManager {
    // 已注册的账号
    private Set<LoginInfo> loginInfos = new HashSet<>();

    private static class SingletonHolder {
        private static UserManager instance = new UserManager();
    }

    private UserManager() {
        LoginInfo wcy = new LoginInfo();
        wcy.setAccount("test1");
        wcy.setToken("123456");
        LoginInfo wcy2 = new LoginInfo();
        wcy2.setAccount("test2");
        wcy2.setToken("123456");
        loginInfos.add(wcy);
        loginInfos.add(wcy2);
    }

    public static UserManager get() {
        return SingletonHolder.instance;
    }

    public boolean verify(LoginInfo loginInfo) {
        for (LoginInfo info : loginInfos) {
            if (info.equals(loginInfo)) {
                return true;
            }
        }
        return false;
    }
}
