package com.jakehilborn.speedr.heremaps;

import com.jakehilborn.speedr.StatsCalculator;
import com.jakehilborn.speedr.heremaps.deserial.HereMapsResponse;
import com.jakehilborn.speedr.utils.UnitUtils;

public class HereMapsManager {

    private final StatsCalculator statsCalculator;
    private String prevRoadName;
    private boolean prevLimitMissing;

    public HereMapsManager(StatsCalculator statsCalculator) {
        this.statsCalculator = statsCalculator;
    }

    public void handleResponse(HereMapsResponse hereMapsResponse, Double lat, Double lon, boolean isUseKph) {
        String roadName = hereMapsResponse.getResponse().getLink()[0].getRoadName();
        if (roadName == null) roadName = "";

        Double limit = parseLimit(hereMapsResponse.getResponse().getLink()[0].getSpeedLimit(), isUseKph);
        if (limit == null) limit = 0D;

        if (limit == 0) {
            if (prevLimitMissing) {
                //Two responses in a row with no speed limit data, set limit to missing.
                statsCalculator.setLimit(0D, lat, lon);
            } else {
                //Don't set limit to missing just yet, wait for the next retry.
                prevLimitMissing = true;
                statsCalculator.setLimit(null, lat, lon);
            }
        } else if (prevRoadName == null || roadName.equals(prevRoadName) || prevLimitMissing) {
            //Normal case, user is on the same road.
            prevRoadName = roadName;
            prevLimitMissing = false;
            statsCalculator.setLimit(limit, lat, lon);
        } else {
            //User changed roads, is currently in intersection, or at an overpass. Since HERE only returns the limit of
            //the one road it thinks you're on we will force a refetch. Don't set limit to missing in case we're only in an
            //intersection or near an overpass.
            prevRoadName = roadName;
            prevLimitMissing = false;
            statsCalculator.setForceLimitStale(true);
        }
    }

    private Double parseLimit(Double limit, boolean isUseKph) {
        //Round Here maps slightly inaccurate speed limit data to nearest 5mph or 5kmh increment.
        if (isUseKph) {
            Integer roundedLimit = UnitUtils.msToKphRoundToFive(limit);
            return UnitUtils.kphToMs(roundedLimit);
        } else { //mph
            Integer roundedLimit = UnitUtils.msToMphRoundToFive(limit);
            return UnitUtils.mphToMs(roundedLimit);
        }
    }
}
