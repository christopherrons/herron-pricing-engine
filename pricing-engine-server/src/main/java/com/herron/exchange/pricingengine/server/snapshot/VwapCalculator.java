package com.herron.exchange.pricingengine.server.snapshot;

import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.Trade;

public class VwapCalculator {

    private Volume totalVolume = Volume.ZERO;
    private Price vwp = Price.ZERO;

    public Price updateAndGetVwap(Trade trade) {
        totalVolume = totalVolume.add(trade.volume());
        vwp = vwp.add(trade.price().multiply(trade.volume().getValue()));
        return vwp.divide(totalVolume.getValue());
    }

    public Price getVwap() {
        return vwp.divide(totalVolume.getValue());
    }

}
