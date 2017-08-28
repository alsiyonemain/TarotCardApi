package com.alexis.tarotapp.api.controllers;

import com.alexis.tarotapp.api.dto.CategoryDto;
import com.alexis.tarotapp.api.dto.DtoHelper;
import com.alexis.tarotapp.api.entities.Category;
import com.alexis.tarotapp.api.general.patch.PATCH;
import com.alexis.tarotapp.api.repository.ICategoryDao;
import com.alexis.tarotapp.api.repository.hibernate.SessionUtil;
import com.alexis.tarotapp.api.general.result.Result;
import com.alexis.tarotapp.api.service.ICategoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.glassfish.jersey.server.ManagedAsync;
import org.hibernate.Session;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alzayon on 8/1/2017.
 */
@Path("categoryresource")
public class CategoryController {

    @Context
    Request request;

    @Context
    ICategoryService categoryService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(Category category) {
        //https://stackoverflow.com/questions/23858488/how-i-return-http-404-json-xml-response-in-jax-rs-jersey-on-tomcat
        final Result<Category> result = categoryService.add(category);
        return Response.ok(DtoHelper.toDto(result.getItem())).build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") final int id, final Category category) {
        if (category.getId() != id) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .header("Error", "The category id does not match the resource path")
                    .build();
        }

        final Result<Category> resultCategory = categoryService.fetch(id);

        if (!resultCategory.empty()) {
            final Result<Category> result = categoryService.update(category);
            return Response.ok(DtoHelper.toDto(result.getItem())).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path("{id}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patch(@PathParam("id") final int id, final JsonPatch document) {
        final Result<Category> resultCategory = categoryService.fetch(id);

        if (!resultCategory.empty()) {
            final Category categoryInstance = resultCategory.getItem();
            final ObjectMapper objectMapper = new ObjectMapper();
            try {
                if (document == null) {
                    throw new IllegalArgumentException("Json patch document is null!");
                }

                final String json = objectMapper.writeValueAsString(categoryInstance);

                //https://stackoverflow.com/questions/3653996/how-to-parse-a-json-string-into-jsonnode-in-jackson
                final JsonNode jsonNode = objectMapper.readTree(json);

                //https://github.com/java-json-tools/json-patch
                final JsonNode appliedJsonNode = document.apply(jsonNode);

                //https://stackoverflow.com/questions/19711695/convert-jsonnode-into-pojo
                final Category category = objectMapper.treeToValue(appliedJsonNode, Category.class);

                final Result<Category> result = categoryService.update(category);

                return Response.ok(DtoHelper.toDto(result.getItem())).build();

            } catch (IOException | JsonPatchException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") final int id) {

        //https://stackoverflow.com/questions/2342579/http-status-code-for-update-and-delete
        //http://allegro.tech/2014/10/async-rest.html

        final Result<Category> resultCategory = categoryService.fetch(id);

        if (!resultCategory.empty()) {
            final Result<Boolean> result = categoryService.delete(resultCategory.getItem());
            if (result.getItem()) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return Response.noContent().build();
        }
    }

    @GET
    @ManagedAsync
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Suspended final AsyncResponse response) {
        final Result<List<Category>> result = categoryService.list();
        final List<CategoryDto> categories = result.getItem().stream()
                .map(DtoHelper::toDto)
                .collect(Collectors.toList());
        return Response.ok(categories).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") int id) {
        final Result<Category> result = categoryService.fetch(id);
        return Response.ok(DtoHelper.toDto(result.getItem())).build();
    }
}