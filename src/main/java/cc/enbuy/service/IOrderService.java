package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;

/**
 * @Author: Pace
 * @Data: 2018/11/29 15:06
 * @Version: v1.0
 */
public interface IOrderService {
    ServerResponse create(Integer id, Integer shippingId);

    ServerResponse cancel(Integer userId, Long orderNo);
    ServerResponse getOrderCartProduct(Integer userId);

    ServerResponse detail(Integer userId, Long orderNo);

    ServerResponse list(Integer id,Integer pageNum,Integer pageSize);
}
