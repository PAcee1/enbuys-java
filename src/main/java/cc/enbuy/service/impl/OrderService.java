package cc.enbuy.service.impl;

import cc.enbuy.common.Const;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.*;
import cc.enbuy.pojo.*;
import cc.enbuy.service.IOrderService;
import cc.enbuy.util.BigDecimalUtil;
import cc.enbuy.util.DateTimeUtil;
import cc.enbuy.util.PropertiesUtil;
import cc.enbuy.vo.OrderItemVo;
import cc.enbuy.vo.OrderProductVo;
import cc.enbuy.vo.OrderVo;
import cc.enbuy.vo.ShippingVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * @Author: Pace
 * @Data: 2018/11/29 15:06
 * @Version: v1.0
 */
@Service
public class OrderService implements IOrderService {

    /**
     * Order ：订单信息
     * OrderItem ：订单中商品信息
     * OrderVo ：订单（包含订单信息，订单中商品信息，订单地址信息）
     * OrderItemVo ：订单Vo需要的订单商品信息（过滤掉一些不需要的商品信息）
     * OrderProductVo：订单商品集合信息，包含List<OrderItemVo>，以及总价与图片
     *
     */

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 创建订单
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse create(Integer userId, Integer shippingId) {
        //从购物车中获取数据
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse serverResponse = getCartOrderItem(userId,cartList);
        //判断返回是否正确
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
        //计算订单总价
        BigDecimal payment = getOrderTotalPrice(orderItemList);
        //生成订单
        Order order = assembleOrder(userId,shippingId,payment);
        if(order ==  null){
            return ServerResponse.createByErrorMessage("创建订单失败");
        }
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        //为订单中商品附上订单号
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //批量插入orderItem到订单商品表
        orderItemMapper.batchInsert(orderItemList);

        //生成成功，需要减少库存
        reduceProductStock(orderItemList);
        //并且需要情空购物车
        cleanCart(cartList);

        //返回前端数据，需要封装对象，将订单信息与订单中商品信息封装起来
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 取消订单
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse<String> cancel(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        //判断订单是否存在
        if(order == null){
            return ServerResponse.createByErrorMessage("您的订单不存在");
        }
        //判断订单状态
        if (order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款，无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode()); // 设置订单状态为已取消
        int rowCount = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(rowCount > 0){
            //说明成功修改订单状态
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    /**
     * 获取订单中商品信息
     * @param userId
     * @return
     */
    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo(); // 使用Vo封装商品信息
        //从购物车中获取商品信息
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        //获取商品信息失败，返回错误信息
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
        List<OrderItemVo> orderItemVoList = Lists.newArrayList(); // 订单商品vo封装信息
        BigDecimal payment = new BigDecimal("0");
        //遍历订单商品，计算总价，并添加封装过的订单商品到集合中
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return ServerResponse.createBySuccess(orderProductVo);

    }

    /**
     * 获取订单详细信息
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse<OrderVo> detail(Integer userId, Long orderNo) {
        //查询订单信息
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            //订单查询失败
            return ServerResponse.createByErrorMessage("没有找到该订单");
        }
        //订单查询成功，查询订单商品信息
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
        //封装到vo中
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 订单列表 需要分页
     * @param id
     * @return
     */
    public ServerResponse<PageInfo> list(Integer id,Integer pageNum,Integer pageSize) {
        //设置分页
        PageHelper.startPage(pageNum,pageSize);
        //获取订单信息的集合
        List<Order> orderList = orderMapper.selectByUserId(id);
        //获取订单集合
        List<OrderVo> orderVoList = assembleOrderVoList(orderList, id);
        PageInfo pageInfo = new PageInfo(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /* 封装订单Vo的集合 */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                //todo 管理员查询的时候 不需要传userId，为了方法重用
                //orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    /* 封装订单信息，订单商品信息，订单收货地址信息到OrderVo中 */
    private OrderVo assembleOrderVo(Order order,List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();
        //添加订单信息
        orderVo.setOrderNo(order.getOrderNo()); // 订单号
        orderVo.setPayment(order.getPayment()); // 支付总价
        orderVo.setPaymentType(order.getPaymentType()); // 支付方式
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue()); // 支付方式描述
        orderVo.setPostage(order.getPostage()); // 运费
        orderVo.setStatus(order.getStatus()); // 支付状态
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue()); // 支付状态描述

        //添加收货地址信息
        orderVo.setShippingId(order.getShippingId()); // 收货地址Id
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId()); // 收货地址信息
        if(shipping != null){ //如果不为空
            orderVo.setReceiverName(shipping.getReceiverName()); // 收货人姓名
            orderVo.setShippingVo(assembleShippingVo(shipping)); // 收货地址信息封装
        }

        //设置时间
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        //设置图片前缀
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        //遍历设置订单商品
        for(OrderItem orderItem : orderItemList){
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    /* 订单商品Vo */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo()); // 订单号
        orderItemVo.setProductId(orderItem.getProductId()); //商品Id
        orderItemVo.setProductName(orderItem.getProductName()); //商品名称
        orderItemVo.setProductImage(orderItem.getProductImage()); // 商品图片
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice()); // 商品价格
        orderItemVo.setQuantity(orderItem.getQuantity()); // 商品数量
        orderItemVo.setTotalPrice(orderItem.getTotalPrice()); // 商品总价
        //创建时间
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    /* 生成订单对象 */
    private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment){
        Order order = new Order();
        //组装订单号
        long orderNo = this.generateOrderNo();
        order.setOrderNo(orderNo); // 订单号
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode()); //组装订单状态
        order.setPostage(0); // 运费
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode()); // 支付方式
        order.setPayment(payment); //支付总价格

        order.setUserId(userId);
        order.setShippingId(shippingId);
        //发货时间，付款时间会在mapper中自动生成

        //向数据库添加新订单
        int rowCount = orderMapper.insert(order);
        //判断订单是否创建成果
        if(rowCount > 0){
            return order;
        }
        return null;
    }

    /* 封装订单商品 */
    private ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        //创建集合用来存放订单中商品
        List<OrderItem> orderItemList = Lists.newArrayList();
        //判断购物车是否为空
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //遍历购物车中商品，使用商品为订单商品赋值
        for (Cart cartItem: cartList) {
            OrderItem orderItem = new OrderItem();
            //查询商品
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            //判断商品是否为售卖状态
            if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
                return ServerResponse.createByErrorMessage("商品"+product.getName()+"不在销售");
            }
            //判断商品库存
            if(product.getStock() < cartItem.getQuantity()){
                return ServerResponse.createByErrorMessage("商品" + product.getName() +"库存不足");
            }

            //为订单商品赋值
            orderItem.setUserId(userId); // 用户id
            orderItem.setProductId(product.getId()); // 商品id
            orderItem.setProductName(product.getName()); // 商品名称
            orderItem.setProductImage(product.getMainImage()); // 商品图片
            orderItem.setCurrentUnitPrice(product.getPrice()); // 商品价格
            orderItem.setQuantity(cartItem.getQuantity()); // 商品数量
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity())); // 商品总价
            //添加到集合中
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }

    /* 封装收货地址 */
    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName()); // 收货人姓名
        shippingVo.setReceiverAddress(shipping.getReceiverAddress()); // 收货地址
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());// 收货地址省份
        shippingVo.setReceiverCity(shipping.getReceiverCity());//  收货地址城市
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict()); // 收货地址区域
        shippingVo.setReceiverMobile(shipping.getReceiverMobile()); // 收货电话
        shippingVo.setReceiverZip(shipping.getReceiverZip()); // 收货邮编
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone()); // 收货手机号
        return shippingVo;
    }

    /* 清空购物车 */
    private void cleanCart(List<Cart> cartList){
        for(Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    /* 订单创建成功后减少库存 */
    private void reduceProductStock(List<OrderItem> orderItemList){
        //遍历减少库存
        for(OrderItem orderItem : orderItemList){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    /* 生成订单号 */
    private long generateOrderNo(){
        //订单号：当前时间毫秒值+随机数
        long currentTime =System.currentTimeMillis();
        return currentTime+new Random().nextInt(100);
    }

    /* 计算订单中商品总价 */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

}
