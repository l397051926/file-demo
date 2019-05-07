package com.gennlife.fs.system.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by Chenjinfeng on 2017/1/5.
 */
@Component("urlBean")
@Scope("singleton")
@ConfigurationProperties(prefix = "urlbean")
public class UrlBean {
    private String knowledge_service_uri = null;
    private String search_service_uri = null;
    private String kdeUrl = null;
    private String searchIndexName = null;
    private String visitIndexName = null;
    private String exportUri;
    private String empiServiceUri;
    private String searchServiceStatisticsUri = null;
    private String searchServicedetailsUri = null;

    public String getEmpiServiceUri() {

        return empiServiceUri;
    }

    public void setEmpiServiceUri(String empiServiceUri) {

        this.empiServiceUri = empiServiceUri;
    }

    public String getKnowledge_service_uri() {
        return knowledge_service_uri;
    }

    public void setKnowledge_service_uri(String knowledge_service_uri) {
        this.knowledge_service_uri = knowledge_service_uri;
    }

    public String getSearch_service_uri() {
        return search_service_uri;
    }

    public void setSearch_service_uri(String search_service_uri) {
        this.search_service_uri = search_service_uri;
    }

    public String getKdeUrl() {
        return kdeUrl;
    }

    public void setKdeUrl(String kdeUrl) {
        this.kdeUrl = kdeUrl;
    }

    public String getSearchIndexName() {
        return searchIndexName;
    }

    public void setSearchIndexName(String searchIndexName) {
        this.searchIndexName = searchIndexName;
    }

    public String getVisitIndexName() {
        return visitIndexName;
    }

    public void setVisitIndexName(String visitIndexName) {
        this.visitIndexName = visitIndexName;
    }

    public void setExportUri(String exportUri) {
        this.exportUri = exportUri;
    }

    public String getExportUri() {
        return exportUri;
    }

    public String getSearchServiceStatisticsUri() {
        return searchServiceStatisticsUri;
    }

    public void setSearchServiceStatisticsUri(String searchServiceStatisticsUri) {
        this.searchServiceStatisticsUri = searchServiceStatisticsUri;
    }

    public String getSearchServicedetailsUri() {
        return searchServicedetailsUri;
    }

    public void setSearchServicedetailsUri(String searchServicedetailsUri) {
        this.searchServicedetailsUri = searchServicedetailsUri;
    }
}
