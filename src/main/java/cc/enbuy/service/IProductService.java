package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.pojo.Product;
import cc.enbuy.vo.ProductDetailVo;
import com.github.pagehelper.PageInfo;

/**
 * @Author: Pace
 * @Data: 2018/11/28 15:19
 * @Version: v1.0
 */
public interface IProductService {

    //获取商品详情
    ServerResponse<ProductDetailVo> getProdcutDetail(Integer productId);

    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy);

    /*-------------------------- 后台 ----------------------------------- */
    //后台保存商品
    ServerResponse saveOrUpdateProduct(Product product);

    ServerResponse setSaleStatus(Integer productId, Integer status);

    ServerResponse manageProductDetail(Integer productId);

    ServerResponse getProductList(int pageNum, int pageSize);

    ServerResponse searchProduct(String productName, Integer productId, int pageNum, int pageSize);
}
