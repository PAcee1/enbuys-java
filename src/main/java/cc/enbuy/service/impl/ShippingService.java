package cc.enbuy.service.impl;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.ShippingMapper;
import cc.enbuy.pojo.Shipping;
import cc.enbuy.service.IShippingService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Author: Pace
 * @Data: 2018/11/29 13:42
 * @Version: v1.0
 */
@Service
public class ShippingService implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 新增地址
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse add(Integer userId, Shipping shipping) {
        //防止越权
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if(rowCount > 0){
            //说明添加成功
            Map result = Maps.newHashMap();
            result.put("shippingId",shipping.getId());
            return ServerResponse.createBySuccess("新增地址成功",result);
        }
        return ServerResponse.createByErrorMessage("新增地址失败");
    }

    /**
     * 删除地址
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse delete(Integer userId, Integer shippingId) {
        /*这样会出现横向越权问题
        int rowCount = shippingMapper.deleteByPrimaryKey(shippingId);*/
        int rowCount = shippingMapper.deleteByShippingIdUserId(shippingId,userId);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("地址删除成功");
        }
        return ServerResponse.createByErrorMessage("地址删除失败");
    }

    /**
     * 修改地址
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse update(Integer userId, Shipping shipping) {
        //防止越权
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("地址修改成功");
        }
        return ServerResponse.createByErrorMessage("地址修改失败");
    }

    /**
     * 查询地址详情
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse select(Integer userId, Integer shippingId) {
        Shipping result = shippingMapper.selectByShippingIdUserId(shippingId,userId);
        if(result == null){
            return ServerResponse.createByErrorMessage("无法查到该地址");
        }
        return ServerResponse.createBySuccess("地址查询成功",result);
    }

    /**
     * 查询地址列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize) {
        //分页
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccess(pageInfo);
    }
}

