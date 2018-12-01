package cc.enbuy.dao;

import cc.enbuy.pojo.Order;
import cc.enbuy.pojo.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    //根据userId和订单号查询订单
    Order selectByUserIdAndOrderNo(@Param("userId")Integer userId,@Param("orderNo")Long orderNo);

    //获取订单集合
    List<Order> selectByUserId(@Param("userId")Integer userId);

    Order selectByOrderNo(Long orderNo);
}