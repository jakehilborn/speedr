package com.jakehilborn.speedr.overpass;

import com.jakehilborn.speedr.StatsCalculator;
import com.jakehilborn.speedr.overpass.deserial.Element;
import com.jakehilborn.speedr.overpass.deserial.OverpassResponse;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.util.HashSet;
import java.util.Set;

public class OverpassManager {

    private final StatsCalculator statsCalculator;
    private String prevRoadName;
    private static final String NO_DATA = "no data";

    public OverpassManager(StatsCalculator statsCalculator) {
        this.statsCalculator = statsCalculator;
    }

    public void handleResponse(OverpassResponse overpassResponse, Double lat, Double lon) {
        Set<LimitNode> limitNodes = normalizeResponse(overpassResponse);

        if (limitNodes.size() == 0) {
            //OpenStreetMaps is missing data for the user's location
            if (prevRoadName != null && prevRoadName.equals(NO_DATA)) {
                //Two responses in a row with no speed limit data, set limit to missing.
                statsCalculator.setLimit(0D, lat, lon);
            } else {
                //Don't set limit to missing just yet, wait for the next retry.
                prevRoadName = NO_DATA;
                statsCalculator.setLimit(null, lat, lon);
            }
        } else if (limitNodes.size() == 1) {
            //Normal case, user is on a single road.
            LimitNode limitNode = limitNodes.iterator().next();
            prevRoadName = limitNode.getRoadName();
            statsCalculator.setLimit(limitNode.getLimit(), lat, lon);
        } else {
            //User at intersection, overpass, or near a parallel road.
            LimitNode sticky = new LimitNode(0D, prevRoadName);
            LimitNode fastest = new LimitNode();

            for (LimitNode limitNode : limitNodes) {
                //If the current response contains 1 or more results matching the road name
                //of the last response then prefer the higher speed limit for that roadName.
                if (limitNode.getRoadName().equals(sticky.getRoadName())) {
                    sticky.setLimit(Math.max(limitNode.getLimit(), sticky.getLimit()));
                }

                //Of all the results in the response find the one with the highest speed limit.
                if (limitNode.getLimit() > fastest.getLimit()) {
                    fastest = limitNode;
                }
            }

            //Since the user is near multiple roads force a limit refetch with forceLimitStale. We will keep refetching
            //while the user is near multiple roads to determine asap which road the user is actually driving on.
            if (sticky.getLimit() != 0) {
                //Use the limit that matches the roadName of previous response.
                statsCalculator.setForceLimitStale(true);
                statsCalculator.setLimit(sticky.getLimit(), lat, lon);
            } else {
                //User is on a new road, use the road that has the fastest speed limit in the response.
                prevRoadName = fastest.getRoadName();
                statsCalculator.setForceLimitStale(true);
                statsCalculator.setLimit(fastest.getLimit(), lat, lon);
            }
        }
    }

    private Set<LimitNode> normalizeResponse(OverpassResponse overpassResponse) {
        Set<LimitNode> limitNodes = new HashSet<>();

        if (overpassResponse == null || overpassResponse.getElements() == null) return limitNodes;

        for (Element element : overpassResponse.getElements()) {
            Double limit = parseLimit(element.getTags().getMaxSpeed());
            String roadName = element.getTags().getName();
            limitNodes.add(new LimitNode(limit, roadName));
        }

        return limitNodes;
    }

    private Double parseLimit(String limit) {
        if (limit == null || limit.isEmpty()) return null;

        //Overpass maxspeed uses whole numbers. Example limit: "35 mph"
        Integer num = Integer.parseInt(limit.replaceAll("[^0-9]", ""));

        if (limit.contains("mph")) {
            return UnitUtils.mphToMs(num);
        } else if (limit.contains("knots")) {
            return UnitUtils.knotsToMs(num);
        } else { //kph if unit is not specified in response
            return UnitUtils.kphToMs(num);
        }
    }
}
