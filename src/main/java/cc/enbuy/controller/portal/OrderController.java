package cc.enbuy.controller.portal;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.User;
import cc.enbuy.service.IOrderService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * @Author: Pace
 * @Data: 2018/11/29 15:05
 * @Version: v1.0
 */
@Controller
@RequestMapping("/myOrder/")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService orderService;

    /**
     * 创建订单
     * @param session
     * @param shippingId
     * @return
     */
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpSession session,Integer shippingId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return orderService.create(user.getId(),shippingId);
    }

    /**
     * 取消订单
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpSession session,Long orderNo){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return orderService.cancel(user.getId(),orderNo);
    }

    /**
     * 获取订单的商品信息
     * @param session
     * @return
     */
    @RequestMapping("getOrderCartProduct.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return orderService.getOrderCartProduct(user.getId());
    }

    /**
     * 订单详情
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpSession session,Long orderNo){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return orderService.detail(user.getId(),orderNo);
    }

    /**
     * 订单列表
     * @param session
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                               @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize,
                               HttpSession session){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return orderService.list(user.getId(),pageNum,pageSize);
    }

    @RequestMapping("category.do")
    @ResponseBody
    public ServerResponse getCategory(){
        return orderService.getCategory();
    }

    /* ---------------------------支付---------------------------------*/

    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        //判断用户是否登录
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        //获取路径，用来临时保存文件
        String path = request.getSession().getServletContext().getRealPath("upload");
        return orderService.pay(orderNo,user.getId(),path);
    }

    /**
     * 支付宝回调函数
     * @return
     */
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();
        Map<String, String[]> requestParams = request.getParameterMap();
        // 获取参数中的key value,支付宝的回调
        for(Iterator iter = requestParams.keySet().iterator();iter.hasNext();){
            String name = (String) iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for(int i=0;i<values.length;i++){
                //判断values中是否有值
                valueStr = (i == values.length -1)?valueStr + values[i]:",";
            }
            params.put(name,valueStr);
        }
        logger.info("支付宝回调，sign:{},trade_status:{},参数:{}",params.get("sign"),
                    params.get("trade_status"),params.toString());

        //验证回调正确性！！！避免重复通知
        params.remove("sign_type");
        try {
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params,
                    Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            if(!alipayRSACheckedV2){
                //如果错误
                return ServerResponse.createByErrorMessage("非法请求");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝回调异常",e);
        }
        //todo 验证各种数据

        ServerResponse serverResponse = orderService.aliCallback(params);
        if (serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }

    /**
     * 查询订单支付状态
     * @param session
     * @return
     */
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        ServerResponse serverResponse = orderService.queryOrderPayStatus(user.getId(),orderNo);
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
