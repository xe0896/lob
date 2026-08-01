package classes;
import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;
import java.util.HashMap;

import enums.Side;

public class Book {
    private final Deque<Order>[] buys;
    private final Deque<Order>[] sells;
    private final HashMap<UUID, Order> orderMap = new HashMap<>();
    private final long pMin;

    // We can derive the idx these live in
    private long bestBidPrice = -1;
    private long bestAskPrice = -1;

    private int sellEntries = 0;
    private int buyEntries = 0;

    @SuppressWarnings("unchecked")
    public Book(long pMin, long pMax) {
        this.pMin = pMin;
        int size = (int)(pMax - pMin + 1);

        buys = (Deque<Order>[]) new Deque[size];
        sells = (Deque<Order>[]) new Deque[size];

        for(int i = 0; i < size; i++) {
            buys[i] = new LinkedList<>();
            sells[i] = new LinkedList<>();
        }
    }

    public void add(Order o) {
        // Calculate offset before we cast, don't cast to int straight away as it may of corrupted already

        if(o.side == Side.BUY) {
            _add(o, buys);
            buyEntries++;
        }
        if(o.side == Side.SELL) {
            _add(o, sells);
            sellEntries++;
        }

        // BUY is bid
        // SELL is ask

        if(o.side == Side.BUY && o.price > bestBidPrice) bestBidPrice = o.price;
        if(o.side == Side.SELL && o.price < bestAskPrice) bestAskPrice = o.price;
    }

    public void _add(Order o, Deque<Order>[] list) {
        long offset = o.price - pMin; 
        if(offset < 0 || offset >= list.length) throw new IllegalArgumentException("Provided order price is out of range");
            
        int idx = (int)(offset);
        orderMap.put(o.id, o);

        list[idx].addLast(o);
    }

    public void cancel(UUID id) {
        Order o = orderMap.get(id);
        orderMap.remove(id);

        int idx = (int)(o.price - pMin);
        if(o.side == Side.BUY) {
            Deque<Order> list = buys[idx];
            list.remove(o);
        } else {
            Deque<Order> list = sells[idx];
            list.remove(o);
        }
    }

    
    public void submit(Order o) {
        // Buyer buys with sellers not with other buyer, given that the current order is a BUY
        // and that its price is more then an ask price meaning it is offering more then being wished for
        // so that is a valid trade, if there was no valid trade then we would add this order to the book since none match yet

        // Mental model: You have $300 and this person is selling for $200 then you would would't mind going and buying for $200 
        if(o.side == Side.BUY) {
            if (bestBidPrice == -1) {
                bestBidPrice = o.price;
                add(o);
                return;
            } else if (bestAskPrice == -1) {
                add(o);
                return;
            }
            // The current bestAskPrice, we calculate its index to get the starting point
            long offset = bestAskPrice - pMin;
            int idx = (int)(offset);
            System.out.println("idx: " + idx);
            Deque<Order> list = sells[idx];
            Order best = list.peekFirst();

            // Given that there is some quantity left on a matching price (matching price means o.price >= askPrice)
            while(best.quantity >= 0) {
                int tradeQty = Math.min(best.quantity, o.quantity);
                o.quantity -= tradeQty;
                best.quantity -= tradeQty;

                // Given that there is no more quantity left for the seller but there is more that we want to buy
                // we need to find the next best thing which would be worser so we increment the price by idx++
                // until we find a price that may work
                if(best.quantity <= 0 && o.quantity > 0) {
                    // There is stuff left so we should try look for more by going upwards since we can't do any better
                    // so we need to look for something more worse but still is within the limits

                    // Increment idx, while it is within the book limit and until it finds a list that isn't size 0
                    // meaning there are some orders to compare with
                    while(idx < buys.length && list.size() == 0) {
                        idx++;
                    }
                    if(idx >= buys.length) {
                        add(o);
                        break;
                    }
                    // Once we find one we get the list and then grab the best one there via peekFirst and check if 
                    // it is a match, if it ain't then we would break and would need to add this to the book
                    list = buys[idx];
                    best = list.peekFirst();
                    if(o.price < best.price) {
                        add(o);
                        break;
                    }
                } else if (o.quantity <= 0) {
                    // If the order quantity ever reaches 0 or negative, then this given order has been fully fulfilled
                    return;
                }
            }        
        } else {
            if (bestAskPrice == -1) {
                bestAskPrice = o.price;
                add(o);
                return;
            } else if (bestBidPrice == -1) {
                add(o);
                return;
            }
        }

        // If there was no match then we would add this order to the book
        // add(o);
    }

    public Order best(Side side) {
        return null;
    }
    

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = false;
        for(int i = 0; i < buys.length; i++) {
            if(buys[i].size() == 0) continue;
            if(i != buys.length - 1 && first) sb.append(", ");
            first = true;
            sb.append(buys[i]);
        }
        sb.append("]");
        if(sellEntries == 0) {
            return sb.toString();
        }
        sb.append("\n");

        first = false;

        sb.append("[");
        for(int i = 0; i < sells.length; i++) {
            if(sells[i].size() == 0) continue;
            if(i != sells.length - 1 && first) sb.append(", ");
            first = true;
            sb.append(sells[i]);
        }
        sb.append("]");
        
        return sb.toString();
    }
}
