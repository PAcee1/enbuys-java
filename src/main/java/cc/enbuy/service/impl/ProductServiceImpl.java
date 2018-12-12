package cc.enbuy.service.impl;

import cc.enbuy.common.Const;
import cc.enbuy.common.ResponseCode;
import cc.enbuy.common.ServerResponse;
import cc.enbuy.dao.CategoryMapper;
import cc.enbuy.dao.ProductMapper;
import cc.enbuy.pojo.Category;
import cc.enbuy.pojo.Product;
import cc.enbuy.service.ICategoryService;
import cc.enbuy.service.IProductService;
import cc.enbuy.util.DateTimeUtil;
import cc.enbuy.util.PropertiesUtil;
import cc.enbuy.vo.ProductDetailVo;
import cc.enbuy.vo.ProductListVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Pace
 * @Data: 2018/11/28 15:19
 * @Version: v1.0
 */
@Service
public class ProductServiceImpl implements IProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ICategoryService categoryService;

    /**
     * 根据商品id获取商品信息
     * @param productId
     * @return
     */
    public ServerResponse<ProductDetailVo> getProdcutDetail(Integer productId){
        //判断productId是非存在
        if(productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        //判断商品是否存在
        if(product == null){
            return ServerResponse.createByErrorMessage("该商品已下架或删除");
        }
        //判断商品状态是非为上架状态
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMessage("该商品已下架或删除");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 根据条件查询商品列表，如关键字，所属分类，分页信息，排序信息
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     */
    @Override
    public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy) {
        // 如果关键字和类别都没有，为错误
        if(StringUtils.isBlank(keyword) && categoryId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //用来保存父节点下所有子分类的集合
        List<Integer> categoryIdList = new ArrayList<Integer>();
        //分类查询
        if (categoryId !=null ){
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if(category == null && StringUtils.isBlank(keyword)){
                //如果没有此分类且没有关键字，返回空结果集
                PageHelper.startPage(pageNum,pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productListVoList);
                return ServerResponse.createBySuccess(pageInfo);
            }

            //如果传的是比较大的父分类，需要获取其所有子分类
            categoryIdList = categoryService.selectCategoryAndChildrenById(category.getId()).getData();
        }
        //关键字处理
        if(StringUtils.isNotBlank(keyword)){
            //处理参数
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        //设定分页信息
        PageHelper.startPage(pageNum,pageSize);
        //排序处理
        if(StringUtils.isNotBlank(orderBy)){
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] orderByArray = orderBy.split("_");
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]);
            }
        }
        //搜索Product
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword
                ,categoryIdList.size()==0?null:categoryIdList);
        List<ProductListVo> productListVoList = Lists.newArrayList();
        //封装商品集合
        for(Product product : productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        //分页
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);

    }

    /*-------------------------- 后台 ----------------------------------- */

    /**
     * 新增、更新商品
     * @param product
     * @return
     */
    public ServerResponse saveOrUpdateProduct(Product product) {
        if (product == null) {
            //如果为空，返回错误信息
            return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
        }
        //判断处理图片
        if (StringUtils.isNotBlank(product.getSubImages())) {
            String[] subImageArray = product.getSubImages().split(",");
            if (subImageArray.length > 0) {
                product.setMainImage(subImageArray[0]);
            }
        }
        //判断是否有商品id，如果有说明新增商品，如果没有有就是新增商品
        if (product.getId() != null) {
            int count = productMapper.updateByPrimaryKey(product);
            if (count > 0) {
                return ServerResponse.createBySuccess("更新产品成功");
            } else {
                return ServerResponse.createByErrorMessage("更新商品失败");
            }
        } else {
            int count = productMapper.insert(product);
            if (count > 0) {
                return ServerResponse.createBySuccess("新增商品成功");
            } else {
                return ServerResponse.createByErrorMessage("新增商品失败");
            }
        }
    }

    /**
     * 修改产品状态
     * @param productId
     * @param status
     * @return
     */
    public ServerResponse setSaleStatus(Integer productId, Integer status) {
        if(null == productId || null == status){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        int count = productMapper.updateByPrimaryKeySelective(product);
        if(count <= 0){
            return ServerResponse.createByErrorMessage("修改商品状态失败");
        }
        return ServerResponse.createBySuccess("修改商品状态成功");
    }

    /**
     * 获取商品详情
     * @param productId
     * @return
     */
    public ServerResponse manageProductDetail(Integer productId) {
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServerResponse.createByErrorMessage("商品不存在");
        }
        //封装商品信息
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 获取商品列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse getProductList(int pageNum, int pageSize) {
        //分页设置
        PageHelper.startPage(pageNum,pageSize);
        List<Product> productList = productMapper.selectList();
        //封装集合
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product : productList) {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        //封装分页
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 查询商品
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse searchProduct(String productName, Integer productId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        //判断按名称是否为空，不为空拼接参数
        if(StringUtils.isNotBlank(productName)){
            //用名称查
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> productList = productMapper.selectByNameAndProductId(productName, productId);
        //封装List
        List<ProductListVo> productListVoList = new ArrayList<>();
        for(Product product:productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 封装商品详细信息
     * @param product
     * @return
     */
    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        //处理所处分类，如果为null，设置为根节点
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            productDetailVo.setParentCategoryId(0);//默认根节点
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());
        }
        //处理时间问题
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }

    /**
     * 封装商品集合
     * @param product
     * @return
     */
    private ProductListVo assembleProductListVo(Product product){
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }
}
