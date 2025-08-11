package com.supermartijn642.tesseract.capabilities;

import java.util.List;

public class Distribution {

    public Object handler;  // serves as an ID to identify the handler
    public long needed;     // how much resources this handler can take
    public long given;      // how much it should be given (update by distributeXXX() methods)

    public Distribution(Object handler, long needed){
        this.handler = handler;
        this.needed = needed;
        given = 0;
    }

    /**
     * Fair distribution of <code>amount</code> among multiple destinations.
     * Each destination receives the same amount of resources. If that fills
     * the destination, the rest is shared equally with the others.
     * @param dests The destinations (or handlers) that need resources
     * @param amount The maximum amount of resources to distribute
     * @return What's left undistributed (0 if everything was distributed).
     */
    public static long distributeFair(List<Distribution> dests, long amount){
        if(dests == null || dests.isEmpty() || amount <= 0)
            return amount;

        dests.sort((a, b) -> Long.compare(b.needed, a.needed));
        int n = dests.size();
        while(n --> 0){
            dests.get(n).given = Math.min(amount / (n+1), dests.get(n).needed);
            amount -= dests.get(n).given;
        }
        return amount;
    }
}
