package cc.enbuy.controller.backend;

import cc.enbuy.common.Const;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;
import cc.enbuy.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * @Author: Pace
 * @Data: 2018/12/3 20:10
 * @Version: v1.0
 */
@Controller
@RequestMapping("/manage/user")
public class UserManageController {

    @Autowired
    private IUserService userService;

    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password , HttpSession session){
        ServerResponse<User> response = userService.login(username,password);
        //判断是否成功登陆
        if(!response.isSuccess()){//false直接返回response
            return response;
        }
        User user = response.getData();
        if(user.getRole() == Const.Role.ROLE_ADMIN){
            //说明登陆的是管理员
            session.setAttribute(Const.CURRENT_USER,user);
            return response;
        }else {// 不是管理员，无法登陆
            return ServerResponse.createByErrorMessage("不是管理员，无法登陆");
        }
    }
}
