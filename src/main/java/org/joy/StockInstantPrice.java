package org.joy;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class StockInstantPrice {
    private String time;
    private double nowPrice;
    private double openPrice;
}
