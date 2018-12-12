package cc.enbuy.controller.backend;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;
import cc.enbuy.service.IStatisticService;
import cc.enbuy.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * @Author: Pace
 * @Data: 2018/12/8 12:21
 * @Version: v1.0
 */
@RequestMapping("/manage/statistic")
@Controller
public class StatisticManageController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IStatisticService statisticService;

    @RequestMapping("base_count.do")
    @ResponseBody
    public ServerResponse getBaseCount(HttpSession session){
        //判断登录状态
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录管理员");

        }
        if(!userService.checkAdminRole(user).isSuccess()){
            return ServerResponse.createByErrorMessage("无权限操作");
        }
        //
        return statisticService.getBaseCount();
    }
}
