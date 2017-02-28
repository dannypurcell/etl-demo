package com.newcontext.data.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.newcontext.data.Cat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Danny Purcell
 */
@Path("/cat")
@Produces(MediaType.APPLICATION_JSON)
public class CatResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatResource.class);

    private Map<String, Cat> catsByName;
    private ListMultimap<String, Cat> catsByFavoriteSpot;

    public CatResource() {
        this.catsByName = new HashMap<>();
        this.catsByFavoriteSpot = ArrayListMultimap.create();
    }

    @POST
    @Timed
    public void createCat(Cat newCat) {
        String name = newCat.getName();
        if (catsByName.containsKey(name)) {
            LOGGER.debug(String.format("%s already exists, updating", name));
            updateCat(newCat);
        } else {
            catsByName.put(name, newCat);
            catsByName.put(newCat.getFavoriteSpot(), newCat);
            LOGGER.debug(String.format("created cat %s", name));
        }
    }

    @GET
    @Timed
    public List<Cat> listCats(@QueryParam("favoriteSpot") String favoriteSpot) {
        return catsByFavoriteSpot.get(favoriteSpot);
    }

    @GET
    @Timed
    @Path("{name}")
    public Cat getCat(@PathParam("name") String name) {
        return catsByName.get(name);
    }

    @PUT
    @Timed
    public Cat updateCat(Cat updatedCat) {
        String name = updatedCat.getName();
        if (!catsByName.containsKey(name)) {
            throw new NotFoundException(String.format("%s not found", name));
        }
        catsByName.put(name, updatedCat);
        catsByFavoriteSpot.put(updatedCat.getFavoriteSpot(), updatedCat);
        LOGGER.debug(String.format("updated cat %s", name));
        return catsByName.get(name);
    }
}
