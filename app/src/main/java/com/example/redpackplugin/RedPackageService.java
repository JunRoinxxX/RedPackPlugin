package com.example.redpackplugin;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

public class RedPackageService extends AccessibilityService {
    private String LAUCHER = "com.tencent.mm.ui.LauncherUI";
    private String LUCKEY_MONEY_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private String LUCKEY_MONEY_RECEIVER = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";

    private static final String TAG = "RedPacketService";

    //聊天界面“微信红包” TextView ID
    private String VIEW_ID_WXHB = "com.tencent.mm:id/r8"; //com.tencent.mm:id/aum
    //聊天界面"已领取" TextView ID
    private String VIEW_ID_RECIEVED = "com.tencent.mm:id/aul";
    //打开红包界面“开” Button ID
    private String VIEW_ID_OPEN = "com.tencent.mm:id/den";
    //得到的红包金额
    private String VIEW_ID_MONEY_AMOUNT = "com.tencent.mm:id/d62";
    //会话列表界面“微信红包” TextView ID
    private String VIEW_ID_CHET_LIST_WXHB = "com.tencent.mm:id/bal";
    //点击红包打开的界面 红包内容描述 TextView ID
    private String VIEW_ID_DESCRIPTION = "com.tencent.mm:id/dam";

    /**
     * 获取PowerManager.WakeLock对象
     */
    private PowerManager.WakeLock wakeLock;

    /**
     * KeyguardManager.KeyguardLock对象
     */
    private KeyguardManager.KeyguardLock keyguardLock;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service进程:: onCreate: ");
    }

    @Override
    protected void onServiceConnected() {
        Toast.makeText(this, "抢红包服务开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
        Log.d(TAG, "Service进程:: onServiceConnected: ");
    }

    //here contains the main 'logic'
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if ((null == event.getPackageName()) || (null == event.getClassName())) {
            return;
        }
        String pkgName = event.getPackageName().toString();
        //如果不是微信
        if (!"com.tencent.mm".equals(pkgName)) {
            return;
        }

        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        Log.i(TAG, "onAccessibilityEvent eventType=" + eventType + " className=" + className);

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        switch (eventType) {
            //通知栏来信息，判断是否含有微信红包字样，是的话跳转
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                for (CharSequence text : texts) {
                    String content = text.toString();
                    Log.i(TAG, "TYPE_NOTIFICATION_STATE_CHANGED content=" + content);
                    if (!TextUtils.isEmpty(content)) {
                        //判断是否含有[微信红包]字样
                        if (content.contains("[微信红包]")) {
                            if (!isScreenOn()) {
                                wakeUpScreen();
                            }
                            //如果有则打开微信红包页面
                            openWeChatPage(event);
                        }
                    }
                }
                break;

            //窗口内容变化监听
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:

                AccessibilityNodeInfo currentRootNode = getRootInActiveWindow();
                Log.d(TAG, "发现Event:onAccessibilityEvent: TYPE_WINDOW_CONTENT_CHANGED");

                Log.d(TAG, "寻找com id：onAccessibilityEvent: " + currentRootNode.findAccessibilityNodeInfosByText("微信红包").getClass().toString());
                if ("android.widget.FrameLayout".contentEquals(event.getClassName())) {
                    //进入开红包界面前会有一个loading界面，之后开界面才刷新出来
                    findNodeInfosByViewId(currentRootNode, VIEW_ID_OPEN);

                    //会话界面来了个红包，只有内容的刷新
                    findNodeInfosByViewId(currentRootNode, VIEW_ID_WXHB);

                    //会话列表来了个红包，只有文字内容的刷新
                    findNodeInfosByViewId(currentRootNode, VIEW_ID_CHET_LIST_WXHB);

                    findNodeInfosByViewId(currentRootNode, VIEW_ID_DESCRIPTION);
                }

                break;

            //界面跳转的监听
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:

                //判断是否是红包领取后的详情界面
                if (LUCKEY_MONEY_DETAIL.equals(className)) {
                    //获取到的红包金额
                    //findNodeInfosByViewId(rootNode,VIEW_ID_MONEY_AMOUNT);
                    performBackClick();
                    //点微信红包后 开的那个界面
                } else if (LUCKEY_MONEY_RECEIVER.equals(className)) {
                    //判断是否是显示‘开’的那个红包界面
                    AccessibilityNodeInfo currentRootNode2 = getRootInActiveWindow();
                    int findResult = findNodeInfosByViewId(currentRootNode2, VIEW_ID_OPEN);
                    Log.i(TAG, "findNodeInfosByViewId findResult=" + findResult);
                    //在开的界面但却没有找到开按钮，红包被别人抢光了
                    if (0 == findResult) {
                        findNodeInfosByViewId(rootNode, VIEW_ID_DESCRIPTION);
                    }
                } else {
                    //微信聊天界面找微信红包
                    findNodeInfosByViewId(rootNode, VIEW_ID_WXHB);

                    //会话列表微信红包
                    findNodeInfosByViewId(rootNode, VIEW_ID_CHET_LIST_WXHB);
                }
                break;
        }
    }


    /**
     * @param rootNode
     * @param viewID
     * @return
     */
    private int findNodeInfosByViewId(AccessibilityNodeInfo rootNode, String viewID) {
        if (null == rootNode) {
            return -1;
        }

        List<AccessibilityNodeInfo> nodeInfoList = rootNode.findAccessibilityNodeInfosByViewId(viewID);
        for (int i = nodeInfoList.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo nodeInfo = nodeInfoList.get(i);
            CharSequence className = nodeInfo.getClassName();
            CharSequence text = nodeInfo.getText();
            CharSequence contentDes = nodeInfo.getContentDescription();
            AccessibilityNodeInfo parent = nodeInfo.getParent();
            Log.i(TAG, "findNodeInfosByViewId viewID=" + viewID + " className=" + className + " text=" + text + " contentDes=" + contentDes);

            //开界面
            if (VIEW_ID_OPEN.equals(viewID)) {
                Log.i(TAG, "VIEW_ID_WXHB 点击开红包");
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return 1;
                //微信红包字样
            } else if (VIEW_ID_WXHB.equals(viewID)) {
                List<AccessibilityNodeInfo> recieved = null;
                if (null != parent) {
                    recieved = parent.getParent().findAccessibilityNodeInfosByViewId(VIEW_ID_RECIEVED);
                }
                //是否已经领过了
                if ((recieved != null) && recieved.size() > 0) {
                    Log.i(TAG, "findNodeInfosByViewId 这个已经领过了, continue");
                    continue;
                }
                Log.i(TAG, "VIEW_ID_WXHB 您有新的红包");
                //while循环,遍历"领取红包"的各个父布局，直至找到可点击的为止
                while (parent != null) {
                    if (parent.isClickable()) {
                        //模拟点击
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return 2;
                    }
                    parent = parent.getParent();
                }
                //红包金额
//            } else if (VIEW_ID_MONEY_AMOUNT.equals(viewID)){
//                return true;
            } else if (VIEW_ID_CHET_LIST_WXHB.equals(viewID)) {
                if (text.toString().contains("[微信红包]")) {
                    //while循环,遍历"领取红包"的各个父布局，直至找到可点击的为止
                    Log.i(TAG, "VIEW_ID_CHET_LIST_WXHB 您有新的红包");
                    while (parent != null) {
                        if (parent.isClickable()) {
                            //模拟点击
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return 3;
                        }
                        parent = parent.getParent();
                    }
                }
            } else if (VIEW_ID_DESCRIPTION.equals(viewID)) {
                if (text.toString().contains("该红包已被领取") || text.toString().contains("红包派完了")) {
                    performBackClick();
                }
                return 4;
            }
        }
        return 0;
    }


    @Override
    public void onInterrupt() {
        Toast.makeText(this, "我快挂了啊---", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Service进程:: onInterrupt: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service进程:: onDestroy: ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "抢红包服务关闭", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Service进程:: onUnbind: ");
        return super.onUnbind(intent);
    }

    /**
     * 模拟返回操作
     */
    public void performBackClick() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "performBackClick");
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /**
     * 开启红包所在的聊天页面
     */
    private void openWeChatPage(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            //打开对应的聊天界面
            PendingIntent pendingIntent = notification.contentIntent;
            Log.i(TAG, "openWeChatPage");
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 返回桌面
     */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    /**
     * 是否为锁屏或黑屏状态
     */
    public boolean isLockScreen() {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        return km.inKeyguardRestrictedInputMode() || !isScreenOn();
    }

    /**
     * 判断是否处于亮屏状态
     *
     * @return true-亮屏，false-暗屏
     */
    public boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    /**
     * 解锁屏幕
     */
    @SuppressLint("InvalidWakeLockTag")
    private void wakeUpScreen() {
        //先将锁释放
        release();
        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //后面的参数|表示同时传入两个值，最后的是调试用的Tag
        wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "bright");
        //点亮屏幕
        wakeLock.acquire();

        //得到键盘锁管理器
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("unlock");
        //解锁
        keyguardLock.disableKeyguard();
        Log.i(TAG, "wakeUpScreen wakeLock=" + wakeLock + " keyguardLock=" + keyguardLock);
    }

    /**
     * 释放keyguardLock和wakeLock
     */
    public void release() {
        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
            keyguardLock = null;
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
