package com.newcontext.etl.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;

/**
 * @author Danny Purcell
 */
public class ETLServerConfiguration extends Configuration {

    private String dataSourceName;
    private String databaseName;
    private String dataSourceUser;
    private String dataSourcePassword;
    private String uploadTargetName;
    private String catFavoritePlace;
    private JerseyClientConfiguration client = new JerseyClientConfiguration();

    @JsonCreator
    public ETLServerConfiguration(@JsonProperty("dataSourceName") String dataSourceName,
                                  @JsonProperty("databaseName") String databaseName,
                                  @JsonProperty("dataSourceUser") String dataSourceUser,
                                  @JsonProperty("dataSourcePassword") String dataSourcePassword,
                                  @JsonProperty("uploadTargetName") String uploadTargetName,
                                  @JsonProperty("catFavoritePlace") String catFavoritePlace) {
        this.dataSourceName = dataSourceName;
        this.databaseName = databaseName;
        this.dataSourceUser = dataSourceUser;
        this.dataSourcePassword = dataSourcePassword;
        this.uploadTargetName = uploadTargetName;
        this.catFavoritePlace = catFavoritePlace;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDataSourceUser() {
        return dataSourceUser;
    }

    public String getDataSourcePassword() {
        return dataSourcePassword;
    }

    public String getUploadTargetName() {
        return uploadTargetName;
    }

    public String getCatFavoritePlace() {
        return catFavoritePlace;
    }

    public JerseyClientConfiguration getClient() {
        return client;
    }

    @Override
    public String toString() {
        return String.format(
                "ETLServerConfiguration{\n" +
                        "dataSourceName='%s',\n" +
                        "databaseName='%s',\n" +
                        "dataSourceUser='%s',\n" +
                        "dataSourcePassword='%s',\n" +
                        "uploadTargetName='%s',\n" +
                        "catFavoritePlace='%s'\n" +
                        "}",
                dataSourceName, databaseName, dataSourceUser, dataSourcePassword, uploadTargetName, catFavoritePlace);
    }
}
