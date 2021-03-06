package com.lijingyao.microservice.coffee.item.service;

import com.lijingyao.microservice.coffee.base.rest.BaseService;
import com.lijingyao.microservice.coffee.base.rest.ServiceResult;
import com.lijingyao.microservice.coffee.item.errors.ItemErrors;
import com.lijingyao.microservice.coffee.item.persistence.entity.Category;
import com.lijingyao.microservice.coffee.item.persistence.entity.ItemInfo;
import com.lijingyao.microservice.coffee.item.persistence.repository.CategoryRepository;
import com.lijingyao.microservice.coffee.item.persistence.repository.ItemRepository;
import com.lijingyao.microservice.coffee.item.restapi.assemblers.ItemAssembler;
import com.lijingyao.microservice.coffee.template.items.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by lijingyao on 2018/7/8 22:30.
 */
@Service
public class ItemService extends BaseService {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemAssembler itemAssembler;

    // TODO validate params
    public ServiceResult<ItemDTO> itemCreative(ItemCreateDTO createDTO) {
        ServiceResult<ItemDTO> result = getResult();

        Category category = categoryRepository.findOne(createDTO.getCatId());
        if (category == null) {
            return result.setErrors(ItemErrors.CATEGORY_NOT_EXIST);
        }

        Optional<ItemInfo> itemOpt = itemAssembler.assembleItemInfo(createDTO);

        if (!itemOpt.isPresent()) {
            return result.setErrors(ItemErrors.ITEM_CREATE_INFO_NULL);
        }

        ItemInfo info = itemRepository.save(itemOpt.get());

        ItemDTO itemDTO = itemAssembler.assembleItemDTO(info, category);
        result.setResult(itemDTO);
        return result;
    }


    public ServiceResult<ItemDTO> getItem(Integer itemId) {
        ServiceResult<ItemDTO> result = getResult();

        ItemInfo info = itemRepository.findOne(itemId);
        if (info == null) {
            return result.setErrors(ItemErrors.ITEM_INFO_NOT_EXIST);
        }
        ItemDTO itemDTO = itemAssembler.assembleItemDTO(info);
        result.setResult(itemDTO);
        return result;
    }


    public ServiceResult<OrderItemPriceDTO> buildItemOrderPrice(OrderItemDTO orderItemDTO) {
        ServiceResult<OrderItemPriceDTO> result = getResult();

        List<OrderItemDetailDTO> detailDTOS = orderItemDTO.getDetailDTOS();
        if (CollectionUtils.isEmpty(detailDTOS)) return result.setErrors(ItemErrors.ITEM_QUERY_PARAM_NULL);

        List<Integer> itemIDS = detailDTOS.stream().map(i -> i.getItemId()).collect(Collectors.toList());
        List<ItemInfo> itemInfos = itemRepository.findAll(itemIDS);

        if (CollectionUtils.isEmpty(itemInfos)) return result.setErrors(ItemErrors.ITEM_INFO_NOT_EXIST);

        if(itemInfos.size() != detailDTOS.size())return result.setErrors(ItemErrors.ITEM_PART_NOT_ENOUGH);

        Map<Integer, ItemInfo> itemInfoMap = itemInfos.stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
        OrderItemPriceDTO orderItemPriceDTO = new OrderItemPriceDTO();

        orderItemPriceDTO.setPriceDTOS(detailDTOS.stream().map(i -> itemAssembler.assembleOrderItemPrice(i, itemInfoMap)).filter(i -> i != null).collect(Collectors.toList()));

        return result.setResult(orderItemPriceDTO);
    }


}
