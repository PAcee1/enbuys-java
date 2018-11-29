package cc.enbuy.service.impl;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.CartMapper;
import cc.enbuy.dao.ProductMapper;
import cc.enbuy.pojo.Cart;
import cc.enbuy.pojo.Product;
import cc.enbuy.service.ICartService;
import cc.enbuy.util.BigDecimalUtil;
import cc.enbuy.util.PropertiesUtil;
import cc.enbuy.vo.CartProductVo;
import cc.enbuy.vo.CartVo;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: Pace
 * @Data: 2018/11/28 17:08
 * @Version: v1.0
 */
@Service
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    /**
     * 添加购物车商品
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    public ServerResponse<CartVo> add(Integer userId,Integer productId,Integer count){
        //校验参数
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据userId和productId向数据库查数据
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        //判断是否存在这样的购物车
        if(cart == null){
            //如果不存在，为该用户的购物车添加此商品
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.CHECKED); // 默认被选中
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem); // 添加此购物车
        }else{
            //存在商品与用户对应关系的购物车，只需将数量添加
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        CartVo cartVo = this.getCartVoList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 修改购物车商品，仅修改数量
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        //校验参数
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据userId和productId向数据库查数据
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        //判断是否存在这样的购物车
        if(cart != null){
            //不存在 设置cart的count
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKeySelective(cart);
        CartVo cartVo = this.getCartVoList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 批量删除购物车商品
     * @param userId
     * @param productIds
     * @return
     */
    public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds) {
        //将多个商品id分开，以集合保存
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList)){ // 如果参数为空，返回错误信息
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        // 如果有参数，批量删除商品
        cartMapper.deleteByUserIdProductIds(userId,productList);
        //从数据库中获取最新的数据
        CartVo cartVo = this.getCartVoList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 查询购物车数据
     * @param userId
     * @return
     */
    public ServerResponse<CartVo> list(Integer userId) {
        CartVo cartVo = this.getCartVoList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 全选与全反选操作
     * @param userId
     * @return
     */
    public ServerResponse<CartVo> selectOrUnselect(Integer userId,Integer productId,Integer checked) {
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        CartVo cartVo = this.getCartVoList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 获取购物车中商品总数
     * @param userId
     * @return
     */
    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            // 如果为空，返回0
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    /**
     * 根据用户id找到所有购物车下的商品，并封装到一起
     * @param userId
     * @return
     */
    private CartVo getCartVoList(Integer userId){
        //用来封装购物车，最大的容器
        CartVo cartVo = new CartVo();
        //获取userId下所有购物车中商品
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        //购物车商品集合，用来封装商品信息
        List<CartProductVo> cartProductVoList = Lists.newArrayList();

        BigDecimal cartTotalPrice = new BigDecimal("0");
        //封装购物车
        if(CollectionUtils.isNotEmpty(cartList)){
            for(Cart cartItem : cartList){
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setProductId(cartItem.getProductId());
                cartProductVo.setUserId(cartItem.getUserId());

                //根据productId获取商品详细信息
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if(product != null){
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());
                    //判断库存
                    int buyLimitCount = 0;
                    if(product.getStock() >= cartItem.getQuantity()){
                        //库存充足的时候
                        buyLimitCount = cartItem.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    }else{
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        //购物车中更新有效库存
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算总价 数量*单价
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    //进行勾选
                    cartProductVo.setProductChecked(cartItem.getChecked());
                }
                //判断是否被勾选
                if(cartItem.getChecked() == Const.Cart.CHECKED){
                    //如果已经勾选,增加到整个的购物车总价中
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);
            }
        }
        cartVo.setCartTotalPrice(cartTotalPrice); // 设置总价
        cartVo.setCartProductVoList(cartProductVoList); // 设置商品集合
        cartVo.setAllChecked(this.getAllCheckedStatus(userId)); //设置勾选状态
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix")); // 设置主图片

        return cartVo;
    }

    /**
     * 判断勾选状态
     * @param userId
     * @return
     */
    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }

}
