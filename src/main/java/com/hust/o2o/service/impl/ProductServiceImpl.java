package com.hust.o2o.service.impl;

import com.hust.o2o.dao.ProductDAO;
import com.hust.o2o.dao.ProductImgDAO;
import com.hust.o2o.dto.ImageHolder;
import com.hust.o2o.dto.ProductExecution;
import com.hust.o2o.enums.ProductStateEnum;
import com.hust.o2o.exceptions.ProductOperationException;
import com.hust.o2o.model.Product;
import com.hust.o2o.model.ProductImg;
import com.hust.o2o.service.ProductService;
import com.hust.o2o.utils.ImageUtil;
import com.hust.o2o.utils.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商品详情 业务操作
 *
 * @author wangleifu
 * @created 2019-01-08 9:43
 */
@Service
public class ProductServiceImpl implements ProductService {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());


    @Autowired
    private ProductDAO productDAO;
    @Autowired
    private ProductImgDAO productImgDAO;

    /**
     * 先添加商品缩略图，然后添加商品，再添加商品详情图
     * @param product         添加到指定的商品
     * @param thumbnail       待添加的商品缩略图
     * @param productImgList  待添加的商品详情图列表
     * @return ProductExecution  DTO数据传输对象
     * @throws ProductOperationException 商品操作异常类型
     */
    @Override
    public ProductExecution addProduct(Product product, ImageHolder thumbnail, List<ImageHolder> productImgList)
            throws ProductOperationException {
        // 空值判断
        if (product != null && product.getShop() != null && product.getShop().getShopId() != null) {
            // 设置商品默认属性
            product.setCreateTime(new Date());
            product.setUpdateTime(new Date());
            // 默认为上架状态
            product.setEnableStatus(1);
            /* 若商品缩略图不为空则添加*/
            if (thumbnail != null) {
                addThumbnail(product, thumbnail);
            }
            try {
                /*添加商品信息*/
                int effectedNum = productDAO.insertProduct(product);
                if (effectedNum <= 0) {
                    logger.info("商品添加失败！");
                    throw new ProductOperationException("商品添加失败！");
                }
            } catch(Exception e) {
                /*商品添加异常处理*/
                logger.info("商品添加失败：" + e.toString());
                throw new ProductOperationException("商品添加失败: " + e.toString());
            }
            /*若商品详情图不为空则添加*/
            if (productImgList.size() > 0) {
                addProductImgList(product, productImgList);
            }
            return new ProductExecution(ProductStateEnum.SUCCESS);
        }
        else {
            /*传参为空则返回空值错误信息*/
            return new ProductExecution(ProductStateEnum.EMPTY);
        }
    }

    /**
     * 通过商品id查询商品细信息
     * @param productId id
     * @return  Product
     */
    @Override
    public Product getProductById(long productId) {
        return productDAO.queryProductById(productId);
    }

    /**
     * 查询指定条件下商品列表并分页，可输入的条件有：商品名（模糊），商品状态，店铺id，商品类别
     * @param product
     * @param pageIndex
     * @param pageSize
     * @return
     * @throws ProductOperationException
     */
    @Override
    public ProductExecution getProductList(Product product, int pageIndex, int pageSize)
            throws ProductOperationException {
        /*计算当前页对应数据库中的行号*/
        int rowIndex = (pageIndex - 1) * pageSize;
        try {
            /*通过service层查询*/
            int count = productDAO.queryProductCount(product);
            List<Product> productList = productDAO.queryProductList(product, rowIndex, pageSize);

            /*查询封装到productExecution，并返回*/
            ProductExecution pe = new ProductExecution(ProductStateEnum.SUCCESS);
            pe.setCount(count);
            pe.setProductList(productList);
            return pe;
        } catch (Exception e) {
            logger.error("查询商品失败：" + e.toString());
            throw new ProductOperationException("查询商品失败：" + e.toString());
        }
    }

    /**
     * 获取指定条件下对应的商品总数
     * @param product
     * @return
     */
    @Override
    public int getProductCount(Product product) { return productDAO.queryProductCount(product); }

    /**
     * 修改商品信息
     * @param product           新的商品属性值
     * @param thumbnail         新的缩略图信息
     * @param productImgList    新的详情图列表
     * @return ProductExecution DTO数据传输对象
     * @throws ProductOperationException 商品操作的异常类型
     */
    @Override
    public ProductExecution modifyProduct(Product product, ImageHolder thumbnail, List<ImageHolder> productImgList)
            throws ProductOperationException {
        // 首先对商品进行空值判定，确认有商品信息传进来再进行下一步处理
        if (product != null && product.getShop() != null && product.getShop().getShopId() != null) {
            product.setUpdateTime(new Date());
            if (thumbnail != null) {
                Product tempProduct = productDAO.queryProductById(product.getProductId());
                if (tempProduct.getImgAddr() != null) {
                    ImageUtil.deleteFileOrPath(tempProduct.getImgAddr());
                }
                addThumbnail(product, thumbnail);
            }
            if (productImgList != null) {
                deleteProductImgList(product.getProductId());
                addProductImgList(product, productImgList);
            }
            try {
                int effectedNum = productDAO.updateProduct(product);
                if (effectedNum <= 0) {
                    throw new ProductOperationException("商品修改失败!");
                }
                else {
                    return new ProductExecution(ProductStateEnum.SUCCESS);
                }
            } catch(Exception e) {
                logger.info("商品修改失败："+e.toString());
                throw new ProductOperationException("商品修改失败: "+e.toString());
            }
        }
        else {
            return new ProductExecution(ProductStateEnum.EMPTY);
        }
    }

    /**
     * 删除指定商品
     * @param productId
     * @return
     * @throws ProductOperationException
     */
    @Override
    public int deleteProduct(long productId) throws ProductOperationException {
        return productDAO.deleteProduct(productId);
    }

    /**
     * 添加商品缩略图
     * @param product   添加到指定的商品
     * @param thumbnail 待添加的图片信息
     */
    private void addThumbnail(Product product, ImageHolder thumbnail) {
        //TODO 添加商品缩略图
        String dest = PathUtil.getShopImagePath(product.getShop().getShopId());
        String thumbnailAddr = ImageUtil.generateThumbnail(thumbnail, dest, 200, 200, 0.8f);
        product.setImgAddr(thumbnailAddr);

    }

    /**
     * 添加商品详情图片
     * @param product         添加到指定的商品
     * @param productImgList  待添加的图片列表
     */
    private void addProductImgList(Product product, List<ImageHolder> productImgList) {
        //TODO 添加商品详情图片
        String dest = PathUtil.getShopImagePath(product.getShop().getShopId());
        List<ProductImg> imgList = new ArrayList<>();
        for (ImageHolder imageHolder : productImgList) {
            String imgAddr = ImageUtil.generateThumbnail(imageHolder, dest, 337, 640, 0.9f);
            ProductImg productImg = new ProductImg();
            productImg.setProductId(product.getProductId());
            productImg.setCreateTime(new Date());
            productImg.setImgAddr(imgAddr);
            productImg.setImgDesc(imageHolder.getImageName());
            productImg.setPriority(1);
            imgList.add(productImg);
        }
        try {
            int effectedNum = productImgDAO.batchInsertProductImg(imgList);
            if (effectedNum <= 0) {
                logger.info("添加商品详情图片失败！");
            }
        }catch (Exception e) {
            logger.info("添加商品详情图片失败：" + e.toString());
        }
    }

    /**
     * 删除指定商品的详情图列表
     * @param productId
     */
    private void deleteProductImgList(long productId) {
        List<ProductImg> productImgList = productImgDAO.queryProductImgList(productId);
        for (ProductImg productImg : productImgList) {
            ImageUtil.deleteFileOrPath(productImg.getImgAddr());
        }
        productImgDAO.deleteProductImgByProductId(productId);
    }
}
