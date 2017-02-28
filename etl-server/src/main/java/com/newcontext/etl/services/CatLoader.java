package com.newcontext.etl.services;

import com.newcontext.data.Cat;
import io.dropwizard.lifecycle.Managed;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Danny Purcell
 */
public class CatLoader implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatLoader.class);

    private DataSource dataSource;
    private WebTarget uploadTarget;
    private ScheduledExecutorService executor;
    private String catFavoritePlace;
    private ScheduledFuture task;

    public CatLoader(DataSource dataSource,
                     WebTarget uploadTarget,
                     ScheduledExecutorService executor,
                     String catFavoritePlace) {
        this.dataSource = dataSource;
        this.uploadTarget = uploadTarget.path("cat");
        this.executor = executor;
        this.catFavoritePlace = catFavoritePlace;
    }

    @Override
    public void start() throws Exception {
        if (task != null) {
            stop();
        }
        this.task = executor.scheduleWithFixedDelay(this::upload, new Random().nextInt(10), 1, TimeUnit.SECONDS);
    }

    private void upload() {
        try {
            Handle h = DBI.open(dataSource);
            List<Cat> batch = Collections.emptyList();
            try {
                LOGGER.debug("loading cats...");
                batch = h.createQuery("select name, age, color from cats")
                         .map((index, r, ctx) -> new Cat(r.getString(1),
                                                         r.getInt(2),
                                                         r.getString(3),
                                                         catFavoritePlace))
                         .list();
            } finally {
                h.close();
            }
            batch.forEach((cat) -> uploadTarget.request().post(Entity.json(cat)));
            LOGGER.debug(String.format("loaded %s cats", batch.size()));
        } catch (Exception e) {
            LOGGER.error("exception during upload", e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (task == null) {
            return;
        }
        task.cancel(true);
        task = null;
    }
}
