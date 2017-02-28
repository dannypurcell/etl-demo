package com.newcontext.etl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.newcontext.data.Cat;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Danny Purcell
 */
public class ETLServerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ETLServerIT.class);

    private static DockerClient docker;
    private static Client client;
    private static WebTarget dataService;
    private static ContainerCreation dataServiceContainer;
    private static ForkJoinTask dataLogger;
    private static ContainerCreation dbContainer;
    private static ContainerCreation etlServiceContainer;

    @BeforeClass
    public static void setup() throws Exception {
        docker = DefaultDockerClient.fromEnv().build();

        dataServiceContainer = docker.createContainer(
                ContainerConfig.builder()
                               .hostConfig(HostConfig.builder().portBindings(
                                       ImmutableMap.of("8080", ImmutableList.of(PortBinding.of("0.0.0.0", "8080")),
                                                       "8081", ImmutableList.of(PortBinding.of("0.0.0.0", "8081"))))
                                                     .build())
                               .portSpecs("8080:8080",
                                          "8081:8081")
                               .exposedPorts("8080", "8081")
                               .image("etl-demo/data-server")
                               .build(),
                "data-server");
        docker.startContainer(dataServiceContainer.id());
        dataLogger = ForkJoinPool.commonPool().submit(() -> {
            try (LogStream stream = docker.logs(dataServiceContainer.id(),
                                                DockerClient.LogsParam.stdout(),
                                                DockerClient.LogsParam.stderr())) {
                while (stream.hasNext()) {
                    LogMessage msg = stream.next();
                    ByteBuffer content = msg.content();
                    byte[] bytes = new byte[content.remaining()];
                    content.get(bytes);
                    LOGGER.debug(String.format("dataServiceContainer: %s",
                                               new String(bytes, Charset.forName("UTF-8"))));
                }
            } catch (Exception e) {
                LOGGER.error("dataServiceContainer: exception in logging task", e);
            }
        });
        client = new JerseyClientBuilder(new MetricRegistry()).using(new JerseyClientConfiguration())
                                                              .using(new ScheduledThreadPoolExecutor(5))
                                                              .using(new ObjectMapper())
                                                              .build("data-server-client");
        dataService = client.target("http://localhost:8080");

        dbContainer = docker.createContainer(
                ContainerConfig.builder()
                               .hostConfig(HostConfig.builder().portBindings(
                                       ImmutableMap.of("3306", ImmutableList.of(PortBinding.of("0.0.0.0", "3306"))))
                                                     .binds(String.format(
                                                             "%s:/docker-entrypoint-initdb.d/test_data.sql",
                                                             Paths.get(Resources.getResource("test_data.sql").toURI())
                                                                  .toFile().getAbsolutePath()))
                                                     .build())
                               .portSpecs("3306:3306")
                               .exposedPorts("3306")
                               .env("MYSQL_ROOT_PASSWORD=secret_squirrel",
                                    "MYSQL_DATABASE=pets")
                               .image("mariadb")
                               .build(),
                "pet-db");
        docker.startContainer(dbContainer.id());

        try (LogStream stream = docker.logs(dbContainer.id(),
                                            DockerClient.LogsParam.stdout(),
                                            DockerClient.LogsParam.stderr())) {
            while (stream.hasNext()) {
                LogMessage msg = stream.next();
                ByteBuffer content = msg.content();
                byte[] bytes = new byte[content.remaining()];
                content.get(bytes);
                if (new String(bytes, Charset.forName("UTF-8")).contains("mysqld: ready for connections")) {
                    break;
                }
            }
        }

        etlServiceContainer = docker.createContainer(
                ContainerConfig.builder()
                               .hostConfig(HostConfig.builder().portBindings(
                                       ImmutableMap.of("9090", ImmutableList.of(PortBinding.of("0.0.0.0", "9090")),
                                                       "9091", ImmutableList.of(PortBinding.of("0.0.0.0", "9091"))))
                                                     .links("data-server", "pet-db")
                                                     .build())
                               .portSpecs("9090:9090",
                                          "9091:9091")
                               .exposedPorts("9090", "9091")
                               .env("DATASOURCE_NAME=PET_DB",
                                    "DATABASE_NAME=pets",
                                    "DATASOURCE_USER=root",
                                    "DATASOURCE_PASSWORD=secret_squirrel",
                                    "UPLOAD_TARGET_NAME=DATA_SERVER",
                                    "CAT_FAVORITE_PLACE=couch")
                               .image("etl-demo/etl-server")
                               .build(),
                "etl-service");
    }

    @AfterClass
    public static void teardown() throws Exception {
        client.close();
        dataLogger.cancel(true);
        docker.killContainer(dataServiceContainer.id());
        docker.removeContainer(dataServiceContainer.id());

        docker.killContainer(dbContainer.id());
        docker.removeContainer(dbContainer.id());

        docker.killContainer(etlServiceContainer.id());
        docker.removeContainer(etlServiceContainer.id());
    }

    @Test
    public void testUploadCats() throws Exception {
        docker.startContainer(etlServiceContainer.id());
        Thread.sleep(10000);

        String[] catNames = new String[]{"spacecat",
                                         "longcat",
                                         "dumpstercat",
                                         "hovercat"};
        int tick = 0;
        int doneCount = 0;
        while (tick <= 50) {
            try {
                doneCount = 0;
                for (String catName : catNames) {
                    Cat cat = dataService.path(String.format("cat/%s", catName))
                                         .request()
                                         .get()
                                         .readEntity(Cat.class);
                    if (cat != null && cat.getFavoriteSpot().equals("couch")) {
                        LOGGER.info(String.format("Found cat: %s", cat));
                        doneCount++;
                    } else {
                        LOGGER.debug(String.format("%s is still hiding", catName));
                    }
                }
                if (doneCount == catNames.length) {
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("Could not fetch cats", e);
            } finally {
                tick++;
                Thread.sleep(500);
            }
        }
        if (doneCount != catNames.length) {
            throw new IllegalStateException("could not find all the cats in time!");
        }
    }
}
