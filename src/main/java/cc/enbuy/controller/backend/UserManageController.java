package cc.enbuy.controller.backend;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;
import cc.enbuy.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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

    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpSession session,
                               @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                               @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(null == user){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(!userService.checkAdminRole(user).isSuccess()){
            return ServerResponse.createByErrorMessage("无权限操作");
        }
        return userService.manageList(pageNum,pageSize);
    }

    /**
     * 用户详情
     * @param session
     * @param userId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public  ServerResponse detail(HttpSession session,int userId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(null == user){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(!userService.checkAdminRole(user).isSuccess()){
            return ServerResponse.createByErrorMessage("无权限操作");
        }
        return userService.manageDetail(userId);
    }

    /**
     * 设置管理员
     * @param session
     * @param userId
     * @return
     */
    @RequestMapping("setUserRole.do")
    @ResponseBody
    public  ServerResponse setUserRole(HttpSession session,int userId,int role){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(null == user){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(!userService.checkAdminRole(user).isSuccess()){
            return ServerResponse.createByErrorMessage("无权限操作");
        }
        return userService.setUserRole(userId,role);
    }
}
