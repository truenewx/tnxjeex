package org.truenewx.tnxjeex.notice.service.sms;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.truenewx.tnxjeex.notice.model.sms.SmsNotifyResult;

/**
 * 短信通知器
 *
 * @author jianglei
 */
public interface SmsNotifier {
    /**
     * 同步通知指定类型的短信
     *
     * @param type       业务类型
     * @param params     参数映射集
     * @param locale     区域
     * @param cellphones 手机号码清单
     * @return 通知结果
     */
    SmsNotifyResult notify(String type, Map<String, Object> params, Locale locale, String... cellphones);

    /**
     * 异步通知指定类型的短信
     *
     * @param type       业务类型
     * @param params     参数映射集
     * @param locale     区域
     * @param cellphones 手机号码清单
     * @param callback   短信通知回调
     */
    void notify(String type, Map<String, Object> params, Locale locale, String[] cellphones,
            Consumer<SmsNotifyResult> callback);

    /**
     * 获取指定业务类型下，对指定手机号码再次发送短信的剩余时间秒数
     *
     * @param type      业务类型
     * @param cellphone 手机号码
     * @return 剩余时间秒数
     */
    int getRemainingSeconds(String type, String cellphone);

}
