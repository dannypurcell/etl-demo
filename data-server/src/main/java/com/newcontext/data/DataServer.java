package com.newcontext.data;

import com.newcontext.data.configuration.DataServerConfiguration;
import com.newcontext.data.health.AvailabilityHealthCheck;
import com.newcontext.data.resources.CatResource;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Danny Purcell
 */
public class DataServer extends Application<DataServerConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataServer.class);

    public static void main(String[] args) throws Exception {
        new DataServer().run(args);
    }

    @Override
    public void initialize(Bootstrap<DataServerConfiguration> bootstrap) {
        bootstrap.addBundle(new TemplateConfigBundle());
    }

    @Override
    public void run(DataServerConfiguration configuration, Environment environment) throws Exception {
        setupRoutes(environment);
        setupHealthChecks(environment);
    }

    private void setupRoutes(Environment environment) {
        environment.jersey().register(new CatResource());
    }

    private void setupHealthChecks(Environment environment) {
        environment.healthChecks().register("availability", new AvailabilityHealthCheck());
    }
}
