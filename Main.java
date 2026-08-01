import classes.Book;
import classes.Order;
import classes.TickConverter;
import enums.Side;

import java.util.UUID;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        TickConverter converter = new TickConverter(new BigDecimal("0.01"));
        // 5000 and 15000 is tick price, so 5000/0.01 = $50
        Book book = new Book(5000, 15000);

        Order o1 = createOrder(Side.BUY, converter.convert(new BigDecimal("150.00")), 7);
        Order o2 = createOrder(Side.SELL, converter.convert(new BigDecimal("100.00")), 10);

        book.submit(o2);
        book.submit(o1);
        
        System.out.println(book);
    }

    public static Order createOrder(Side side, long price, int quantity) {
        return new Order(UUID.randomUUID(), side, price, quantity);
    }
}
