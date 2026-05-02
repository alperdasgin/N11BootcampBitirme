package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.StockUpdateRequest;
import com.ecommerce.stock_service.dto.StockUpdateResponse;

public interface StockDomainService {

    StockUpdateResponse reserve(StockUpdateRequest req);

    StockUpdateResponse release(StockUpdateRequest req);

    StockUpdateResponse commit(StockUpdateRequest req);
}
