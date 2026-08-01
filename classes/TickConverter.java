package classes;
import java.math.BigDecimal;
import java.math.RoundingMode;


public class TickConverter {
    private final BigDecimal tick; // Decimal number use BigDecimal for no rounding errors

    public TickConverter(BigDecimal tick) {
        this.tick = tick;
    }

    public long convert(BigDecimal price) {
        return price.divide(tick, 0, RoundingMode.UNNECESSARY).longValueExact();
    }
}
