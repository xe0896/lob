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
    private long bestAskPrice = Integer.MAX_VALUE;

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

        if(o.side == Side.BUY && o.price > bestBidPrice) {
            bestBidPrice = o.price;
            System.out.println("BUY");
        }
        if(o.side == Side.SELL && o.price < bestAskPrice) {
            bestAskPrice = o.price;
            System.out.println("SELL");
        }
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

        System.out.println("o: " + o);

        int idx = (int)(o.price - pMin);
        if(o.side == Side.BUY) {
            buys[idx].remove(o);
        } else {
            sells[idx].remove(o);
        }
    }

    
    public void submit(Order o) {
        // Buyer buys with sellers not with other buyer, given that the current order is a BUY
        // and that its price is more then an ask price meaning it is offering more then being wished for
        // so that is a valid trade, if there was no valid trade then we would add this order to the book since none match yet

        // Mental model: You have $300 and this person is selling for $200 then you would would't mind going and buying for $200 
        if(o.side == Side.BUY) {
            System.out.println("sellEntries: " + sellEntries);
            if (sellEntries == 0) {
                // No sell entries mean we won't find a match so just add straight away
                System.out.println("o.buy getting added");
                add(o);
                return;
            }
            // The current bestAskPrice, we calculate its index to get the starting point
            long offset = bestAskPrice - pMin;
            int idx = (int)(offset); 
            Deque<Order> list = sells[idx];
            Order best = list.peekFirst();

            if(best == null) {
                add(o);
                return;
            }

            // Given that there is some quantity left on a matching price (matching price means o.price >= askPrice)
            while(idx < sells.length) {
                if(best != null && o.price >= best.price) {
                    // Two scenarios here, if best.quantity was the minimum then it would of taken it all
                    // making it zero, else o.quantity would be zero and there is more left in the 
                    // best.quantity which is handled by then if-branch below
                    int tradeQty = Math.min(best.quantity, o.quantity);
                    System.out.println("reached");
                    o.quantity -= tradeQty;
                    best.quantity -= tradeQty;
                }  

                System.out.println("best.quantity: " + best.quantity);
                System.out.println("o.quantity: " + o.quantity);
                System.out.println(best);
                System.out.println("o.price: " + o.price);
                System.out.println("best.price: " + best.price);

                // Given that there is no more quantity left for the seller but there is more that we want to buy
                // we need to find the next best thing which would be worser so we increment the price by idx++
                // until we find a price that may work
                if(best.quantity <= 0 && o.quantity > 0) {
                    System.out.println('f');
                    // There is stuff left so we should try look for more by going upwards since we can't do any better
                    // so we need to look for something more worse but still is within the limits

                    // Increment idx, while it is within the book limit and until it finds a list that isn't size 0
                    // meaning there are some orders to compare with
                    System.out.println("idx: " + idx);
                    System.out.println("sells.length: " + sells.length);
                    System.out.println(list);
                    while(idx <= sells.length && list.size() == 0) {
                        System.out.println("?");
                        idx++;
                    }

                    if(idx > sells.length) {
                        add(o);
                        return;
                    }
                    
                    // Once we find one we get the list and then grab the best one there via peekFirst and check if 
                    // it is a match, if it ain't then we would break and would need to add this to the book
                    list = sells[idx];
                    best = list.peekFirst();
                    if(o.price < best.price) {
                        add(o);
                        break;
                    }
                } else if (best.quantity == 0 && o.quantity == 0) {
                    System.out.println("raeCHE");
                    cancel(best.id);
                } else if (o.quantity <= 0) {
                    // If the order quantity ever reaches 0 or negative, then this given order has been fully fulfilled
                    return;
                }
            }        
        } else {
            if (buyEntries == 0) {
                add(o);
                return;
            }
            // The current bestAskPrice, we calculate its index to get the starting point
            long offset = bestBidPrice - pMin;
            int idx = (int)(offset);
            Deque<Order> list = buys[idx];
            Order best = list.peekFirst();

            if(best == null) {
                add(o);
                return;
            }

            // Given that there is some quantity left on a matching price (matching price means o.price >= askPrice)
            while(idx >= 0) {
                if(o.price <= best.price) {
                    // Two scenarios here, if best.quantity was the minimum then it would of taken it all
                    // making it zero, else o.quantity would be zero and there is more left in the 
                    // best.quantity which is handled by then if-branch below
                    int tradeQty = Math.min(best.quantity, o.quantity);
                    o.quantity -= tradeQty;
                    best.quantity -= tradeQty;
                }  

                System.out.println("best.quantity: " + best.quantity);
                System.out.println("o.quantity: " + o.quantity);
                System.out.println(best);
                System.out.println("o.price: " + o.price);
                System.out.println("ebest.price: " + best.price);

                // Given that there is no more quantity left for the seller but there is more that we want to buy
                // we need to find the next best thing which would be worser so we increment the price by idx++
                // until we find a price that may work
                if(best.quantity <= 0 && o.quantity > 0) {
                    System.out.println("best: " + best);
                    cancel(best.id);
                    // There is stuff left so we should try look for more by going upwards since we can't do any better
                    // so we need to look for something more worse but still is within the limits

                    // Increment idx, while it is within the book limit and until it finds a list that isn't size 0
                    // meaning there are some orders to compare with
                    while(idx >= 0 && list.size() == 0) {
                        idx--;
                    }

                    if(idx < 0) {
                        add(o);
                        System.out.println('e');
                        return;
                    }
                   
                    // Once we find one we get the list and then grab the best one there via peekFirst and check if 
                    // it is a match, if it ain't then we would break and would need to add this to the book
                    list = buys[idx];
                    best = list.peekFirst();
                    if(o.price < best.price) {
                        add(o);
                        break;
                    }
                } else if (best.quantity == 0 && o.quantity == 0) {
                    System.out.println("raeCHE");
                    cancel(best.id);
                } else if (o.quantity <= 0) {
                    // If the order quantity ever reaches 0 or negative, then this given order has been fully fulfilled
                    return;
                }
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
        StringBuilder sb = new StringBuilder("BUYS: \n");
        boolean first = false;
        System.out.println(buys.length);
        for(int i = 0; i < buys.length; i++) {
            if(buys[i].size() == 0) continue;
            if(i != buys.length - 1 && first) sb.append("\n");
            first = true;
            sb.append(buys[i]);
        }
        if(sellEntries == 0) return sb.toString();
    
        sb.append("\nSELLS: \n");

        first = false;

        for(int i = 0; i < sells.length; i++) {
            if(sells[i].size() == 0) continue;
            if(i != sells.length - 1 && first) sb.append("\n");
            first = true;
            sb.append(sells[i]);
        }
        
        return sb.toString();
    }
}
