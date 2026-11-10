package com.finanzapp.movements.service.domain;

import java.math.BigDecimal;
import java.util.List;

public record SharedFinanceSummary(
        BigDecimal personalNetWorth,
        BigDecimal sharedNetWorth,
        BigDecimal combinedNetWorth,
        List<SharedFinanceMember> members) {

    public record SharedFinanceMember(String email, BigDecimal netWorth) {
    }
}
