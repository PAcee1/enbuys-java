package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;

/**
 * @Author: Pace
 * @Data: 2018/11/24 21:15
 * @Version: v1.0
 */
public interface IUserService {
    ServerResponse<User> login(String username, String password);
    ServerResponse<String> register(User user);
    ServerResponse<String> checkValid(String str,String type);

    ServerResponse<String> forgetGetQuestion(String username);

    ServerResponse<String> forgetCheckAnswer(String username, String question, String answer);

    ServerResponse<String> forgetResetPassword(String username, String newPassword, String token);

    ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user);

    ServerResponse<User> updateInformation(User currentUser, User user);

    ServerResponse<User> getInformation(Integer id);
}
