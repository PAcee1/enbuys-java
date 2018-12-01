package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;
import cc.enbuy.vo.ProductDetailVo;
import com.github.pagehelper.PageInfo;

/**
 * @Author: Pace
 * @Data: 2018/11/28 15:19
 * @Version: v1.0
 */
public interface IProductService {

    ServerResponse<ProductDetailVo> getProdcutDetail(Integer productId);

    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy);
}
