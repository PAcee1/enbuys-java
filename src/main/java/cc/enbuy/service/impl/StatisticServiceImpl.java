package cc.enbuy.service.impl;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.OrderMapper;
import cc.enbuy.dao.ProductMapper;
import cc.enbuy.dao.UserMapper;
import cc.enbuy.service.IStatisticService;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: Pace
 * @Data: 2018/12/8 12:25
 * @Version: v1.0
 */
@Service
public class StatisticServiceImpl implements IStatisticService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;

    /**
     * 统计信息并返回
     * @return
     */
    public ServerResponse getBaseCount() {
        int userCount = userMapper.selectCount();
        int productCount = productMapper.selectCount();
        int orderCount = orderMapper.selectCount();
        Map result = Maps.newHashMap();
        result.put("userCount",userCount);
        result.put("productCount",productCount);
        result.put("orderCount",orderCount);
        return ServerResponse.createBySuccess(result);
    }
}
