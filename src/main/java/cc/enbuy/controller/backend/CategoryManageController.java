package cc.enbuy.controller.backend;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;
import cc.enbuy.service.ICartService;
import cc.enbuy.service.ICategoryService;
import cc.enbuy.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * @Author: Pace
 * @Data: 2018/12/3 20:18
 * @Version: v1.0
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManageController {

    @Autowired
    private IUserService userService;
    @Autowired
    private ICategoryService categoryService;

    /**
     * 添加品类
     * @param session
     * @param categoryName
     * @param parentId
     * @return
     */
    @RequestMapping("addCategory.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session, String categoryName,
                                      @RequestParam(value = "parentId",defaultValue = "0")Integer parentId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
        }
        //校验是否为管理员
        if(!userService.checkAdminRole(user).isSuccess()){
            //不是管理员
            return ServerResponse.createByErrorMessage("没有管理员权限，不得操作");
        }
        return categoryService.addCategory(categoryName,parentId);
    }

    /**
     * 修改品类
     * @param session
     * @param categoryName
     * @param categoryId
     * @return
     */
    @RequestMapping("setCategoryName.do")
    @ResponseBody
    public ServerResponse setCategoryName(HttpSession session, String categoryName,Integer categoryId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
        }
        //校验是否为管理员
        if(!userService.checkAdminRole(user).isSuccess()){
            //不是管理员
            return ServerResponse.createByErrorMessage("没有管理员权限，不得操作");
        }
        return categoryService.setCategoryName(categoryName,categoryId);
    }


    @RequestMapping("getCategory.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpSession session, @RequestParam(value = "categoryId",defaultValue = "0")Integer categoryId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
        }
        //校验是否为管理员
        if(!userService.checkAdminRole(user).isSuccess()){
            //不是管理员
            return ServerResponse.createByErrorMessage("没有管理员权限，不得操作");
        }
        //查询子节点，并且不递归,保持平级
        return categoryService.getChildrenParallelCategory(categoryId);
    }

    /**
     * 递归查询子节点
     * @param session
     * @param categoryId
     * @return
     */
    @RequestMapping("getDeepCategory.do")
    @ResponseBody
    public ServerResponse getDeepCategory(HttpSession session, @RequestParam(value = "categoryId",defaultValue = "0")Integer categoryId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
        }
        //校验是否为管理员
        if(!userService.checkAdminRole(user).isSuccess()){
            //不是管理员
            return ServerResponse.createByErrorMessage("没有管理员权限，不得操作");
        }
        //递归查询子节点
        return categoryService.selectCategoryAndChildrenById(categoryId);
    }
}
