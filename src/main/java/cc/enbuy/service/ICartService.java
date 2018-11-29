package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.vo.CartVo;

/**
 * @Author: Pace
 * @Data: 2018/11/28 17:08
 * @Version: v1.0
 */
public interface ICartService {
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);

    ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count);

    ServerResponse<CartVo> deleteProduct(Integer userId, String productIds);

    ServerResponse<CartVo> list(Integer userId);

    ServerResponse<CartVo> selectOrUnselect(Integer userId,Integer productId,Integer checked);

    ServerResponse<Integer> getCartProductCount(Integer userId);
}
