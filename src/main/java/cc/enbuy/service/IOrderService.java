package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;

import java.util.Map;

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

    /* ---------------------------支付---------------------------------*/

    ServerResponse pay(Long orderNo,Integer userId,String path);
    ServerResponse aliCallback(Map<String,String> params);

    ServerResponse queryOrderPayStatus(Integer id, Long orderNo);

    /* ---------------------------后台---------------------------------*/
    ServerResponse manageList(int pageNum, int pageSize);

    ServerResponse manageSearch(Long orderNO, int pageNum, int pageSize);

    ServerResponse manageDetail(Long orderNo);

    ServerResponse sendGoods(Long orderNo);

    ServerResponse getCategory();
}
