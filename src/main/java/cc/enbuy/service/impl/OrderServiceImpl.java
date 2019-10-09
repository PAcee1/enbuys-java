package cc.enbuy.service.impl;

import cc.enbuy.common.Const;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.*;
import cc.enbuy.pojo.*;
import cc.enbuy.service.IOrderService;
import cc.enbuy.util.BigDecimalUtil;
import cc.enbuy.util.DateTimeUtil;
import cc.enbuy.util.FTPUtil;
import cc.enbuy.util.PropertiesUtil;
import cc.enbuy.vo.*;
import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: Pace
 * @Data: 2018/11/29 15:06
 * @Version: v1.0
 */
@Service
public class OrderServiceImpl implements IOrderService {

    /**
     * Order ：订单信息
     * OrderItem ：订单中商品信息
     * OrderVo ：订单（包含订单信息，订单中商品信息，订单地址信息）
     * OrderItemVo ：订单Vo需要的订单商品信息（过滤掉一些不需要的商品信息）
     * OrderProductVo：订单商品集合信息，包含List<OrderItemVo>，以及总价与图片
     *
     */
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

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
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CategoryMapper categoryMapper;

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
        //设置订单关闭时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        updateOrder.setCloseTime(DateTimeUtil.strToDate(dateString));
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
        //注意要放入订单信息集合
        PageInfo pageInfo = new PageInfo(orderList);
        //再设置vo
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 检验回调字段
     * @param params
     * @return
     */
    public ServerResponse aliCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("非嗯买商城的订单,回调忽略");
        }
        //判断订单状态
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        //判断回调是否是交易成功
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment"))); // 交易时间
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }

        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo); // 支付宝交易号码
        payInfo.setPlatformStatus(tradeStatus); // 支付宝回调状态

        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();
    }

    /**
     * 判断订单状态是否已支付
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse queryOrderPayStatus(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    /* ---------------------------后台---------------------------------*/

    /**
     * 后台获取分页订单
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse manageList(int pageNum, int pageSize) {
        //分页
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        //封装结果集
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,null);
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 后台按订单号查询订单
     * @param orderNo
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse manageSearch(Long orderNo, int pageNum, int pageSize) {
        //分页
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("订单不存在");
        }
        //订单存在，查询订单中商品
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
        //封装
        OrderVo orderVo = assembleOrderVo(order, orderItemList);
        PageInfo pageInfo = new PageInfo(Lists.newArrayList(order));
        pageInfo.setList(Lists.newArrayList(orderVo));
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 后台查询订单详情
     * @param orderNo
     * @return
     */
    public ServerResponse manageDetail(Long orderNo) {
        //查询订单信息
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            //订单查询失败
            return ServerResponse.createByErrorMessage("没有找到该订单");
        }
        //订单查询成功，查询订单商品信息
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
        //封装到vo中
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 发货
     * @param orderNo
     * @return
     */
    public ServerResponse sendGoods(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return  ServerResponse.createByErrorMessage("未找到该订单");
        }
        //修改订单状态
        order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
        //修改订单发货时间
        order.setSendTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(order);
        if(count <= 0 ){
            return ServerResponse.createByErrorMessage("发货失败");
        }
        return ServerResponse.createBySuccess("发货成功");
    }

    @Override
    public ServerResponse getCategory() {
        List<Category> categoryList = categoryMapper.selectParent();
        CategoryVo vo = new CategoryVo();
        vo.setCartVoList(categoryList);
        return ServerResponse.createBySuccess(vo);
    }

    /* 封装订单Vo的集合 */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                //管理员查询的时候 不需要传userId，为了方法重用
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
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
            orderVo.setReceiverPhone(shipping.getReceiverPhone()); // 收货人电话
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

    /* ---------------------------支付---------------------------------*/

    public ServerResponse pay(Long orderNo,Integer userId,String path) {
        Map<String, String> resultMap = Maps.newHashMap();
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));
        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("嗯买商城扫码支付，订单号：").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo, userId);
        for (OrderItem orderItem : orderItemList) {
            // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
            GoodsDetail goods1 = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            // 创建好一个商品后添加至商品明细列表
            goodsDetailList.add(goods1);
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        // 支付宝当面付2.0服务
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File folder = new File(path);
                if (!folder.exists()) {
                    folder.setWritable(true);
                    folder.mkdirs();
                }


                // 需要修改为运行机器上的路径
                String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo()); // 生成二维码，重要！
                String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                File targetFile = new File(path, qrFileName);
                try {
                    FTPUtil.uploadFile(Lists.<File>newArrayList(targetFile));
                } catch (IOException e) {
                    log.error("上传二维码异常", e);
                }
                log.info("qrPath:" + qrPath);
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                resultMap.put("qrUrl",qrUrl);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");
            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }

}
