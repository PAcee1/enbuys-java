package cc.enbuy.controller.portal;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.service.IProductService;
import cc.enbuy.vo.ProductDetailVo;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author: Pace
 * @Data: 2018/11/28 15:17
 * @Version: v1.0
 */
@Controller
@RequestMapping("/myProduct/")
public class ProductController {

    @Autowired
    private IProductService procutService;

    /**
     * 根据id获取商品详情
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId){
        return procutService.getProdcutDetail(productId);
    }

    /**
     * 根据条件查询商品列表，是否排序，分页信息，类别
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value = "keyword",required = false)String keyword,
                                         @RequestParam(value = "categoryId",required = false)Integer categoryId,
                                         @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,
                                         @RequestParam(value = "orderBy",defaultValue = "") String orderBy){
        return procutService.getProductByKeywordCategory(keyword,categoryId,pageNum,pageSize,orderBy);
    }

}
