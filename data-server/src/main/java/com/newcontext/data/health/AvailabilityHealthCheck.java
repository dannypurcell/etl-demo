package com.newcontext.data.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * @author Danny Purcell
 */
public class AvailabilityHealthCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
