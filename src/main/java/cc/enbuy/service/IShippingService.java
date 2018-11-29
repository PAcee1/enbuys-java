package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.Shipping;
import com.github.pagehelper.PageInfo;

/**
 * @Author: Pace
 * @Data: 2018/11/29 13:41
 * @Version: v1.0
 */
public interface IShippingService {
    ServerResponse add(Integer userId, Shipping shipping);

    ServerResponse delete(Integer userId, Integer shippingId);

    ServerResponse update(Integer userId, Shipping shipping);

    ServerResponse select(Integer userId, Integer shippingId);

    ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize);
}
