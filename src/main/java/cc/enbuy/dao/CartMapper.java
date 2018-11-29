package cc.enbuy.dao;

import cc.enbuy.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    //根据userId和productId查购物车信息
    Cart selectCartByUserIdProductId(@Param("userId") Integer userId, @Param("productId")Integer productId);

    //查询userId下购物车中所有商品
    List<Cart> selectCartByUserId(@Param("userId")Integer userId);

    //获取商品被选中状态
    int selectCartProductCheckedStatusByUserId(@Param("userId")Integer userId);

    //根据多个id删除购物车中商品
    int deleteByUserIdProductIds(@Param("userId") Integer userId, @Param("productIdList")List<String> productIdList);

    //修改选中与被选中
    int checkedOrUncheckedProduct(@Param("userId") Integer userId,@Param("productId")Integer productId,@Param("checked") Integer checked);

    //获取购物车中商品数量
    int selectCartProductCount(@Param("userId") Integer userId);

    //根据userId获取购物车中被选中的商品，用来生成订单
    List<Cart> selectCheckedCartByUserId(Integer userId);
}