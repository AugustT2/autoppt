package com.example.pptrefresh.funds;

import com.example.pptrefresh.query.ManagerTenureRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 占位实现：按基金代码查询成立日/任职日，当前均返回固定演示值。
 * TODO: 对接真实基金主数据或持仓接口。
 */
@Component
public class StubFundFactsClient implements FundFactsClient {

    private static final LocalDate STUB_INCEPTION = LocalDate.of(2019, 6, 12);
    private static final LocalDate STUB_MANAGER_TENURE = LocalDate.of(2020, 1, 15);
    private static final String STUB_LATEST_SCALE = "62.8";

    @Override
    public Optional<LocalDate> fetchInceptionDate(String fundCode) {
        // TODO: GET /funds/{fundCode}/inception-date
        if (!StringUtils.hasText(fundCode)) {
            return Optional.empty();
        }
        return Optional.of(STUB_INCEPTION);
    }

    @Override
    public Optional<LocalDate> fetchManagerTenureStart(String fundCode, ManagerTenureRule tenureRule) {
        // TODO: GET /funds/{fundCode}/manager-tenure?rule={tenureRule}
        if (!StringUtils.hasText(fundCode)) {
            return Optional.empty();
        }
        return Optional.of(STUB_MANAGER_TENURE);
    }

    @Override
    public Optional<String> fetchLatestScale(String fundCode) {
        // TODO: GET /funds/{fundCode}/latest-scale
        if (!StringUtils.hasText(fundCode)) {
            return Optional.empty();
        }
        return Optional.of(STUB_LATEST_SCALE);
    }
}
