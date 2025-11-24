package com.garganttua.core.runtime.perfs;

import java.time.Duration;

import com.garganttua.core.runtime.RuntimeResult;

public record TestPerfReport(int runs, int results, long avg, long min, long max, long total, Duration duration) {






    public void print(){
        System.out.println("===================================");
        System.out.println("Nb        : " + runs);
        System.out.println("Results   : " + results);
        System.out.println("Loss rate : " + lossRate());
        System.out.println("Moyenne   : " + RuntimeResult.prettyNano(avg));
        System.out.println("Min       : " + RuntimeResult.prettyNano(min));
        System.out.println("Max       : " + RuntimeResult.prettyNano(max));
        System.out.println("Total     : " + RuntimeResult.prettyNano(total));
        System.out.println("Test total time    : " + RuntimeResult.prettyDurationColor(duration));
    }

    public int lossRate() {
        return (runs-results)/runs*100;
    }

}
