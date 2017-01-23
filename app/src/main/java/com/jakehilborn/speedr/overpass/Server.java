package com.jakehilborn.speedr.overpass;

import java.util.Arrays;

public class Server {
    private String baseUrl; //Base url of the server
    private long delay; //Delay until the time to use this endpoint again. System elapsed time in nanos.
    private long[] latencies; //Latency times for last 5 successful requests in milliseconds.
    private int ptr; //pointer for circular array of latencies

    public Server(String baseUrl) {
        this.baseUrl = baseUrl;
        latencies = new long[5];
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getLatency() {
        int count = 0;
        long result = 0;

        for (long latency : latencies) {
            if (latency != 0) count++;
            result += latency;
        }

        if (count == 0) return 0; //Avoid divide by 0
        return result / count;
    }

    public void addLatency(long latency) {
        ptr = (ptr + 1) % 5; //increment or wrap around to index 0
        latencies[ptr] = latency;
    }

    public void clearLatencies() {
        latencies = new long[5];
    }

    @Override
    public String toString() {
        return "Server{" +
                "baseUrl='" + baseUrl + '\'' +
                ", delay=" + delay +
                ", latencies=" + Arrays.toString(latencies) +
                ", ptr=" + ptr +
                ", latency=" + getLatency() +
                ", delayDiff=" + (delay - System.nanoTime()) / 1000000000L +
                '}';
    }
}
