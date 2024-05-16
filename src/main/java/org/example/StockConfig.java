package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockConfig {
    private String stockName;
    private String stockCode;
    private Double expectedBid; // expected bid price
    private Double expectedAsk; // expected ask price
    private boolean havePosition; // Whether I hold a position
}