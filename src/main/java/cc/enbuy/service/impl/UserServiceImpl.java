package cc.enbuy.service.impl;

import cc.enbuy.common.Const;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.common.TokenCache;
import cc.enbuy.dao.UserMapper;
import cc.enbuy.pojo.User;
import cc.enbuy.service.IUserService;
import cc.enbuy.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @Author: Pace
 * @Data: 2018/11/24 21:16
 * @Version: v1.0
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     * @param username
     * @param password
     * @return
     */
    public ServerResponse<User> login(String username, String password) {
        //判断此username是否存在
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0){
            //如果不存在，返回错误信息
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //存在，密码登录是MD5加密
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username,md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);
    }

    /**
     * 用户注册
     * @param user
     * @return
     */
    public ServerResponse<String> register(User user) {
        //判断用户名是否重复
        ServerResponse validResponse = this.checkValid(user.getUsername(),Const.USERNAME);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        //判断email是否重复
        validResponse = this.checkValid(user.getEmail(),Const.EMAIL);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        //为用户添加role，为消费者
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //对密码进行MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        //插入数据库
        int count = userMapper.insert(user);
        if(count == 0){
            return ServerResponse.createByErrorMessage("内部原因注册失败");
        }

        return ServerResponse.createBySuccessMsg("注册成功");
    }

    /**
     * 校验手机或email
     * @param str
     * @param type
     * @return
     */
    public ServerResponse<String> checkValid(String str,String type) {
        if(StringUtils.isNotBlank(type)){
            int count;
            if(type.trim().equals(Const.USERNAME)){
                //判断用户名是否重复
                count = userMapper.checkUsername(str);
                if (count > 0){
                    return ServerResponse.createByErrorMessage("用户名重复");
                }
            }
            if(type.trim().equals(Const.EMAIL)) {
                //判断email是否重复
                count = userMapper.checkEmail(str);
                if (count > 0) {
                    return ServerResponse.createByErrorMessage("邮箱重复");
                }
            }
        }else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMsg("校验成功");
    }

    /**
     * 根据用户名查找密码问题
     * @param username
     * @return
     */
    public ServerResponse<String> forgetGetQuestion(String username) {
        //判断用户名是否为空
        int count = userMapper.checkUsername(username);
        if (count == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //调用获取问题
        String question = userMapper.selectQuestionByUsername(username);
        //判断问题是否为空
        if(StringUtils.isBlank(question)){
            return ServerResponse.createByErrorMessage("密码提示问题为空");
        }
        return ServerResponse.createBySuccess(question);
    }

    /**
     * 判断密码提示问题答案是否正确
     * @param username
     * @param question
     * @param answer
     * @return
     */
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        //调用方法
        int count = userMapper.checkAnswer(username,question,answer);
        //判断答案是否正确，如果结果为1说明正确
        if(count > 0){
            //生成token
            String forgetToken = UUID.randomUUID().toString();
            //将token保持到本地缓存
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("提示问题的答案错误");
    }

    /**
     * 修改新密码
     * @param username
     * @param newPassword
     * @param forgetToken
     * @return
     */
    public ServerResponse<String> forgetResetPassword(String username, String newPassword, String forgetToken) {
        //如果token为空，返回错误信息
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("未传递Token");
        }
        //判断用户名是否存在
        int count = userMapper.checkUsername(username);
        if (count == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //判断本地token是否存在并且和传入token相同
        String loaclToken = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(loaclToken)){
            return ServerResponse.createByErrorMessage("token无效或过期");
        }else if(StringUtils.equals(forgetToken,loaclToken)){ //token相同
            //密码md5加密
            String md5Password = MD5Util.MD5EncodeUtf8(newPassword);
            count = userMapper.updatePasswordByUsername(username, md5Password);
            //判断是否修改成功
            if(count > 0){
                return ServerResponse.createBySuccessMsg("密码修改成功");
            }
        }else {
            return ServerResponse.createByErrorMessage("token错误，请重新校验提示问题答案");
        }

        return ServerResponse.createByErrorMessage("密码修改失败");
    }

    /**
     * 登录状态下修改密码
     * @param passwordOld
     * @param passwordNew
     * @param user
     * @return
     */
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        //先判断用户id与旧密码是否一致
        int count = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(count == 0){ // 不一致返回错误信息
            return ServerResponse.createByErrorMessage("原密码错误");
        }
        //一致进行update
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        count = userMapper.updateByPrimaryKeySelective(user);
        if(count == 0){
            return ServerResponse.createByErrorMessage("密码修改失败");
        }
        return ServerResponse.createBySuccessMsg("密码修改成功");
    }

    /**
     * 登录状态下修改用户信息
     * @param currentUser
     * @param user
     * @return
     */
    public ServerResponse<User> updateInformation(User currentUser, User user) {
        //将id与username传给新user
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        //校验新email是否存在，并且是否属于其他人
        int count = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(count > 0){
            return ServerResponse.createByErrorMessage("email已经存在，请更换");
        }
        //更新用户信息
        count = userMapper.updateByPrimaryKeySelective(user);
        if(count == 0){
            return ServerResponse.createBySuccess("更新个人信息成功",user);
        }
        return ServerResponse.createBySuccessMsg("用户信息修改成功");
    }

    /**
     * 获取当前用户信息
     * @param id
     * @return
     */
    public ServerResponse<User> getInformation(Integer id) {
        //根据id获取用户信息
        User user = userMapper.selectByPrimaryKey(id);
        //判断user是否存在
        if(user == null){
            ServerResponse.createByErrorMessage("用户不存在");
        }
        //设置密码为空
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }


}
