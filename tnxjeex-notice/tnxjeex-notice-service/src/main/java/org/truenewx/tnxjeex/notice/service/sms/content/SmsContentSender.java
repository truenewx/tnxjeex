package org.truenewx.tnxjeex.notice.service.sms.content;

import org.truenewx.tnxjeex.notice.model.sms.SmsNotifyResult;

/**
 * 短信内容发送器<br/>
 * 仅负责发送提供的短信内容，不管内容如何生成
 *
 * @author jianglei
 */
public interface SmsContentSender {

    /**
     * @return 支持的业务类型集合
     */
    String[] getTypes();

    /**
     * @return 给同一个手机号码发送短信的间隔时间秒数，默认：60
     */
    default int getIntervalSeconds() {
        return 60;
    }

    /**
     * 发送短信
     *
     * @param signName     签名
     * @param content      短信内容
     * @param maxCount     内容拆分的最大条数
     * @param mobilePhones 手机号码清单
     * @return 短信发送结果
     */
    SmsNotifyResult send(String signName, String content, int maxCount, String... mobilePhones);

}
