package cc.enbuy.service;

import cc.enbuy.common.ServerResponse;

import java.util.List;

/**
 * @Author: Pace
 * @Data: 2018/11/28 15:50
 * @Version: v1.0
 */
public interface ICategoryService {

    ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId);
}
