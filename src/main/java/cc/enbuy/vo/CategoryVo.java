package cc.enbuy.vo;

import cc.enbuy.pojo.Cart;
import cc.enbuy.pojo.Category;

import java.util.List;

/**
 * @Author: Pace
 * @Data: 2019/4/13 13:46
 * @Version: v1.0
 */
public class CategoryVo {

    private List<Category> cartVoList;

    public void setCartVoList(List<Category> cartVoList) {
        this.cartVoList = cartVoList;
    }

    public List<Category> getCartVoList() {

        return cartVoList;
    }
}
