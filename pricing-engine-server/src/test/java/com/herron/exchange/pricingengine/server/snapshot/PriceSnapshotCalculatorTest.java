package com.herron.exchange.pricingengine.server.snapshot;

import com.herron.exchange.common.api.common.api.referencedata.instruments.BondInstrument;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.enums.QuoteTypeEnum;
import com.herron.exchange.common.api.common.messages.common.*;
import com.herron.exchange.common.api.common.messages.pricing.ImmutableBondDiscountPriceModelParameters;
import com.herron.exchange.common.api.common.messages.refdata.*;
import com.herron.exchange.common.api.common.messages.trading.*;
import com.herron.exchange.pricingengine.server.theoretical.TheoreticalPriceCalculator;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.BondPriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.herron.exchange.common.api.common.enums.CompoundingMethodEnum.COMPOUNDING;
import static com.herron.exchange.common.api.common.enums.DayCountConventionEnum.ACT365;
import static com.herron.exchange.common.api.common.enums.EventType.SYSTEM;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.ASK;
import static com.herron.exchange.common.api.common.enums.OrderSideEnum.BID;
import static com.herron.exchange.common.api.common.enums.PriceType.*;
import static com.herron.exchange.common.api.common.enums.TradeType.AUTOMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceSnapshotCalculatorTest {

    private PriceSnapshotCalculator calculator;
    private TheoreticalPriceCalculator theoreticalPriceCalculator = new TheoreticalPriceCalculator(new BondPriceCalculator(null), null, null);

    @BeforeEach
    void before() {
        var instrument = ImmutableDefaultEquityInstrument.builder()
                .instrumentId("instrumendId")
                .firstTradingDate(Timestamp.from(LocalDate.MIN))
                .lastTradingDate(Timestamp.from(LocalDate.MAX))
                .product(ImmutableProduct.builder().currency("eur").productId("product").market(ImmutableMarket.builder().marketId("market").businessCalendar(BusinessCalendar.defaultWeekendCalendar()).build()).build())
                .priceModelParameters(ImmutableIntangiblePriceModelParameters.builder().build())
                .build();
    }

    @Test
    void test_last_price() {
        calculator = new PriceSnapshotCalculator(buildInstrument(List.of(LAST_PRICE)), theoreticalPriceCalculator);
        var marketDataPrice = calculator.updateAndGet(createTrade(1, 10, 10));
        assertEquals(Price.create(10), marketDataPrice.price());
    }

    @Test
    void test_vwap_price() {
        calculator = new PriceSnapshotCalculator(buildInstrument(List.of(VWAP)), theoreticalPriceCalculator);
        var marketDataPrice = calculator.updateAndGet(createTrade(1, 10, 10));
        marketDataPrice = calculator.updateAndGet(createTrade(50000, 12, 10));
        assertEquals(Price.create(11), marketDataPrice.price().scale(1));
    }

    @Test
    void test_mid_bid_ask_price() {
        calculator = new PriceSnapshotCalculator(buildInstrument(List.of(MID_BID_ASK_PRICE)), theoreticalPriceCalculator);
        var marketDataPrice = calculator.updateAndGet(createQuote(1, 10, ASK));
        marketDataPrice = calculator.updateAndGet(createQuote(50000, 12, BID));
        assertEquals(Price.create(11), marketDataPrice.price().scale(1));
    }

    @Test
    void test_throttling_filter() {
        calculator = new PriceSnapshotCalculator(buildInstrument(List.of(VWAP)), theoreticalPriceCalculator);
        var marketDataPrice = calculator.updateAndGet(createTrade(1, 10, 10));
        marketDataPrice = calculator.updateAndGet(createTrade(2, 12, 10));
        assertNull(marketDataPrice);
    }

    @Test
    void test_price_priority() {
        calculator = new PriceSnapshotCalculator(buildInstrument(List.of(BID_PRICE, LAST_PRICE, MID_BID_ASK_PRICE, THEORETICAL, BID_PRICE)), theoreticalPriceCalculator);
        var marketDataPrice = calculator.updateAndGet(createTrade(1, 9, 10));
        var marketDataPrice2 = calculator.updateAndGet(createQuote(10000, 10, BID));
        assertEquals(Price.create(10), marketDataPrice2.price().scale(1));
    }

    private BondInstrument buildInstrument(List<PriceType> intradayPricePriority) {
        return ImmutableDefaultBondInstrument.builder()
                .instrumentId("instrumentId")
                .couponAnnualFrequency(2)
                .maturityDate(Timestamp.from(LocalDate.of(2023, 1, 1)))
                .startDate(Timestamp.from(LocalDate.of(2021, 1, 1)))
                .nominalValue(MonetaryAmount.create(1000, "eur"))
                .couponRate(PureNumber.create(0.05))
                .priceModelParameters(ImmutableBondDiscountPriceModelParameters.builder().dayCountConvention(ACT365)
                        .compoundingMethod(COMPOUNDING)
                        .calculateWithCurve(false)
                        .constantYield(0.03)
                        .intradayPricePriority(intradayPricePriority)
                        .yieldCurveId("id")
                        .build()
                )
                .product(buildProduct(BusinessCalendar.noHolidayCalendar()))
                .firstTradingDate(Timestamp.from(LocalDate.MIN))
                .lastTradingDate(Timestamp.from(LocalDate.MAX))
                .build();
    }

    private Product buildProduct(BusinessCalendar businessCalendar) {
        return ImmutableProduct.builder()
                .productId("product")
                .businessCalendar(businessCalendar)
                .market(buildMarket(businessCalendar))
                .currency("eur")
                .build();
    }

    private Market buildMarket(BusinessCalendar businessCalendar) {
        return ImmutableMarket.builder()
                .marketId("market")
                .businessCalendar(businessCalendar)
                .build();
    }

    private Trade createTrade(long timeOfEventMs, double price, double volume) {
        return ImmutableTrade.builder()
                .bidParticipant(new Participant("m", "u"))
                .askParticipant(new Participant("m", "u"))
                .tradeId("tradeId")
                .bidOrderId("bidOrderId")
                .askOrderId("askOrderId")
                .isBidSideAggressor(true)
                .volume(Volume.create(volume))
                .price(Price.create(price))
                .timeOfEvent(Timestamp.from(timeOfEventMs))
                .instrumentId("instrumentId")
                .orderbookId("orderbookId")
                .eventType(SYSTEM)
                .tradeType(AUTOMATCH)
                .build();
    }

    private TopOfBook createQuote(long timeOfEventMs, double price, OrderSideEnum side) {
        var quote = ImmutablePriceQuote.builder()
                .quoteType(side == BID ? QuoteTypeEnum.BID_PRICE : QuoteTypeEnum.ASK_PRICE)
                .timeOfEvent(Timestamp.from(timeOfEventMs))
                .orderbookId("orderbookId")
                .eventType(SYSTEM)
                .price(Price.create(price))
                .build();

        var builder = ImmutableTopOfBook.builder()
                .timeOfEvent(Timestamp.from(timeOfEventMs))
                .orderbookId("orderbookId")
                .eventType(SYSTEM);

        return side == BID
                ? builder.bidQuote(quote).build()
                : builder.askQuote(quote).build();
    }
}