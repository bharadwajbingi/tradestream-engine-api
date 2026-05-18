package com.mphasis.tse.mapper;

import com.mphasis.tse.entity.TradeTransaction;
import org.mapstruct.Mapper;
import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TradeTransactionMapper {
    default TradeTransaction toEntity(String[] row) {
        TradeTransaction t = new TradeTransaction();
        t.setTransactionId(row[0]);
        t.setFileHeaderDate(row[1]);
        t.setAccountNumber(row[2]);
        t.setTransactionType(Integer.valueOf(row[3]));
        t.setBatchLocation(row[4]);
        t.setBatchNumber(Integer.valueOf(row[5]));
        t.setUpdateBatchDate(row[6]);
        t.setRelatedFileNumber(Integer.valueOf(row[7]));
        t.setActionName(row[8]);
        t.setRelatedFileKey(Long.valueOf(row[9]));
        t.setDoNotReportFlag(row[10]);
        t.setExplanation(row[11]);
        t.setMinorAssetsClass(Integer.valueOf(row[12]));
        t.setOwningPortfolio(Integer.valueOf(row[13]));
        t.setPosterInitials(row[14]);
        t.setTransactionSubtype(Integer.valueOf(row[15]));
        t.setCashEffect(new BigDecimal(row[16]));
        t.setCashPaidOut(new BigDecimal(row[17]));
        t.setBrokerNumber(Integer.valueOf(row[18]));
        t.setOldBalance(new BigDecimal(row[19]));
        t.setNewBalance(new BigDecimal(row[20]));
        return t;
    }
}
