package com.newbie.core.aop;

import com.alibaba.fastjson.JSON;
import com.newbie.constants.NewbieBootInfraConstants;
import com.newbie.core.audit.CurrentUserContext;
import com.newbie.context.UserInfoManager;
import lombok.extern.java.Log;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.rpc.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @Author: 谢海龙
 * @Date: 2019/5/22 14:06
 * @Description
 */
@Configuration
@Component
@Log
public class UserInfoForDubboFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        try{
            String userInfo = RpcContext.getContext().getAttachment(NewbieBootInfraConstants.CURRENT_USER_INFO);
            if (StringUtils.isNotBlank(userInfo)) {
                var currentUserInfo = JSON.parseObject(userInfo, CurrentUserContext.class);
                UserInfoManager.getInstance().bind(currentUserInfo);
            }
            result = invoker.invoke(invocation);
            return result;
        }
        finally {
            UserInfoManager.getInstance().remove();
        }
    }
}



