package me.wcy.cchat;

import me.wcy.cchat.model.LoginInfo;

/**
 * Created by hzwangchenyan on 2017/12/26.
 */
public class AppCache {
    private static PushService service;
    private static LoginInfo myInfo;

    public static PushService getService() {
        return service;
    }

    public static void setService(PushService service) {
        AppCache.service = service;
    }

    public static LoginInfo getMyInfo() {
        return myInfo;
    }

    public static void setMyInfo(LoginInfo myInfo) {
        AppCache.myInfo = myInfo;
    }
}
