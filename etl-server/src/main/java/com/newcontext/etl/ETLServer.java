package com.newcontext.etl;

import com.newcontext.etl.configuration.ETLServerConfiguration;
import com.newcontext.etl.health.AvailabilityHealthCheck;
import com.newcontext.etl.services.CatLoader;
import com.zaxxer.hikari.HikariDataSource;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Danny Purcell
 */
public class ETLServer extends Application<ETLServerConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ETLServer.class);

    public static void main(String[] args) throws Exception {
        new ETLServer().run(args);
    }

    @Override
    public void initialize(Bootstrap<ETLServerConfiguration> bootstrap) {
        bootstrap.addBundle(new TemplateConfigBundle());
    }

    @Override
    public void run(ETLServerConfiguration configuration, Environment environment) throws Exception {
        setupServices(configuration, environment);
        setupHealthChecks(environment);
    }

    private void setupServices(ETLServerConfiguration configuration, Environment environment) {
        LOGGER.debug(String.format("env: %s", System.getenv()));
        LOGGER.debug(String.format("configuration: %s", configuration));

        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(20);
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");

        String dataSourceHost = System.getenv(String.format("%s_PORT_3306_TCP_ADDR",
                                                            configuration.getDataSourceName()));
        String dataSourceURL = String.format("jdbc:mysql://%s:3306/%s",
                                             dataSourceHost,
                                             configuration.getDatabaseName());
        LOGGER.debug(String.format("dataSourceURL: %s", dataSourceURL));
        dataSource.setJdbcUrl(dataSourceURL);

        dataSource.addDataSourceProperty("user", configuration.getDataSourceUser());
        dataSource.addDataSourceProperty("password", configuration.getDataSourcePassword());

        Client client = new JerseyClientBuilder(environment).using(configuration.getClient())
                                                            .build(getName());

        String uploadTargetHost = String.format("http://%s:8080",
                                                System.getenv(String.format("%s_PORT_8080_TCP_ADDR",
                                                                            configuration.getUploadTargetName())));
        LOGGER.debug(String.format("uploadTargetHost: %s", uploadTargetHost));
        environment.lifecycle().manage(new CatLoader(dataSource,
                                                     client.target(uploadTargetHost),
                                                     new ScheduledThreadPoolExecutor(5),
                                                     configuration.getCatFavoritePlace()));
    }

    private void setupHealthChecks(Environment environment) {
        environment.healthChecks().register("availability", new AvailabilityHealthCheck());
    }
}
