package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * name (3x boost), description ve category alanlarında full-text arama yapar.
     * Fuzziness AUTO ile yazım hatalarını tolere eder.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "term": { "active": true } },
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["name^3", "description", "category^2"],
                      "fuzziness": "AUTO"
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
}
