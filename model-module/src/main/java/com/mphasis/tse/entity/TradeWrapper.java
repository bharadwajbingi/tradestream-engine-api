package com.mphasis.tse.entity;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TradeWrapper {

    private TradeTransaction tradeTransaction;
    private List<TransactionError> errors;

    public TradeWrapper() {
        this.errors = new ArrayList<>();
    }


}