package com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds;

import com.herron.exchange.common.api.common.api.referencedata.exchange.BusinessCalendar;
import com.herron.exchange.common.api.common.api.referencedata.exchange.Product;
import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.CompoundingMethodEnum;
import com.herron.exchange.common.api.common.enums.DayCountConvetionEnum;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableHerronBondInstrument;
import com.herron.exchange.common.api.common.messages.refdata.ImmutableHerronProduct;
import com.herron.exchange.common.api.common.model.HerronBusinessCalendar;
import com.herron.exchange.pricingengine.server.curves.YieldCurve;
import com.herron.exchange.pricingengine.server.curves.YieldRefData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BondDiscountingPriceModelTest {

    private BondDiscountingPriceModel bondPriceModel;

    @BeforeEach
    void init() {
        bondPriceModel = new BondDiscountingPriceModel();
    }

    @Test
    void test_zero_yield_compounding_interest_with_accrued_interest() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.04,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2021, 6, 30);
        var result = bondPriceModel.calculateBondPrice(bond, 0, now);
        assertEquals(19.90, result.accruedInterest(), 0.1);
    }

    @Test
    void test_constant_yield_compounding_interest_with_accrued_interest() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2031, 1, 1),
                LocalDate.of(2011, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2011, 4, 30);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(16.43, result.accruedInterest(), 0.01);
    }

    @Test
    void test_zero_coupon_pricing() {
        var bond = buildInstrument(
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.00,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2019, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.05, now);
        assertEquals(376.89, result.cleanPrice(), 0.1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2021, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.05,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar()));

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.03, now);
        assertEquals(1038.54, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_2() {
        var bond = buildInstrument(
                1,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(796.14, result.cleanPrice(), 0.01);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest_pricing_3() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.04, now);
        assertEquals(798.83, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    @Test
    void test_constant_yield_compounding_interest__30360_pricing_4() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2028, 10, 1),
                LocalDate.of(2023, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.015,
                DayCountConvetionEnum.BOND_BASIS_30360,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, 0.1, now);
        assertEquals(677.91, result.cleanPrice(), 0.01);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0.00, result.accruedInterest(), 0.001);
    }

    @Test
    void test_bond_price_with_curve() {
        var bond = buildInstrument(
                2,
                LocalDate.of(2040, 1, 1),
                LocalDate.of(2020, 1, 1),
                1000,
                CompoundingMethodEnum.COMPOUNDING,
                0.025,
                DayCountConvetionEnum.ACT365,
                buildProduct(HerronBusinessCalendar.noHolidayCalendar())
        );

        YieldCurve curve = createTestCurve();
        LocalDate now = LocalDate.of(2020, 1, 1);
        var result = bondPriceModel.calculateBondPrice(bond, curve, now);
        assertEquals(812.32, result.cleanPrice(), 1);
        assertEquals(result.cleanPrice(), result.dirtyPrice(), 0.01);
        assertEquals(0, result.accruedInterest(), 0);
    }

    private Product buildProduct(BusinessCalendar businessCalendar) {
        return ImmutableHerronProduct.builder()
                .productId("product")
                .businessCalendar(businessCalendar)
                .build();
    }

    private BondInstrument buildInstrument(int frequency,
                                           LocalDate maturityData,
                                           LocalDate startDate,
                                           double nominalValue,
                                           CompoundingMethodEnum compoundingMethodEnum,
                                           double couponRate,
                                           DayCountConvetionEnum dayCountConvetionEnum,
                                           Product product) {
        return ImmutableHerronBondInstrument.builder()
                .instrumentId("instrumentId")
                .couponAnnualFrequency(frequency)
                .maturityDate(maturityData)
                .startDate(startDate)
                .compoundingMethod(compoundingMethodEnum)
                .nominalValue(nominalValue)
                .couponRate(couponRate)
                .dayCountConvention(dayCountConvetionEnum)
                .product(product)
                .build();
    }

    private YieldCurve createTestCurve() {
        LocalDate startDate = LocalDate.parse("2019-01-01");
        List<LocalDate> maturityDates = new ArrayList<>();
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 2));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 3));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 4));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 5));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 10));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 20));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 30));
        maturityDates.add(startDate.plusDays((long) YieldRefData.DAYS_PER_YEAR * 50));
        double[] yields = new double[]{0.01, 0.015, 0.02, 0.03, 0.035, 0.035, 0.04, 0.04, 0.045};
        return new YieldCurve(new YieldRefData(LocalDate.parse("2019-01-01"), maturityDates, yields));
    }
}