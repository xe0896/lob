package classes;
import enums.Side;
import java.util.UUID;

public class Order {
    public UUID id;
    public Side side;
    public long price;
    public int quantity;
    public int sequenceNum;

    public static int sequenceNumStatic = 0;

    // price is tickPrice, in units determined by the tick
    public Order(UUID id, Side side, long price, int quantity) {
        this.id = id;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.sequenceNum = sequenceNumStatic;

        Order.sequenceNumStatic++;
    }

    @Override
    public String toString() {
        return "id=%s, side=%s, price=%d, qty=%d, seq=%d".formatted(id, side, price, quantity, sequenceNum);    
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        if(!(other instanceof Order)) return false;

        Order o = (Order) other;

        return id == o.id;
    }
}