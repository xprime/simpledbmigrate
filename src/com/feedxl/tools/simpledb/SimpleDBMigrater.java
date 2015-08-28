package com.feedxl.tools.simpledb;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.feedxl.tools.aws.SimpleDBConnectionHelper;
import com.feedxl.tools.util.CommandLineOptionsProcessor;

import java.io.IOException;
import java.util.List;

/**
 * Created by vikram on 27/08/15.
 */
public class SimpleDBMigrater {

    private final CommandLineOptionsProcessor commandLineOptionsProcessor;

    public static void main(String [] args) throws IOException {
        SimpleDBMigrater migrater = new SimpleDBMigrater(args);
        migrater.migrate();
        System.exit(0);
    }

    public SimpleDBMigrater(String[] args) throws IOException {
        commandLineOptionsProcessor = new CommandLineOptionsProcessor(args);
        if (!commandLineOptionsProcessor.processInput()) {
            System.exit(-1);
        }
        if (!commandLineOptionsProcessor.validateMandatoryFields("sourceRegionName",
                "destinationRegionName", "destinationProfile")) {
            System.exit(-1);
        }
        if (!commandLineOptionsProcessor.validateFieldsTogether("startYear","endYear")) {
            System.exit(-1);
        }
        commandLineOptionsProcessor.populateDefaultsIfMissing("sourceProfile", "default",
                "timeFilter", "", "truncate", "false");
    }

    public void migrate() throws IOException {
        if (commandLineOptionsProcessor.hasParam("domainName")) {
            String domainName = commandLineOptionsProcessor.getString("domainName");
            migrateDomain(domainName);
        } else {
            for (String domainName : getDomains()) {
                migrateDomain(domainName);
            }
        }
    }

    private List<String> getDomains() {
        AmazonSimpleDBClient connection = getSourceConnection();
        ListDomainsRequest request = new ListDomainsRequest();
        ListDomainsResult result = connection.listDomains(request);
        return result.getDomainNames();
    }

    private AmazonSimpleDBClient getSourceConnection() {
        String regionName = commandLineOptionsProcessor.getString("sourceRegionName");
        String profile = commandLineOptionsProcessor.getString("sourceProfile");
        return new SimpleDBConnectionHelper().createSimpleDBConnection(profile, regionName);
    }

    private AmazonSimpleDBClient getDestinationConnection() {
        String regionName = commandLineOptionsProcessor.getString("destinationRegionName");
        String profile = commandLineOptionsProcessor.getString("destinationProfile");
        return new SimpleDBConnectionHelper().createSimpleDBConnection(profile, regionName);
    }

    private void migrateDomain(String domainName) throws IOException {
        String sourceRegionName = commandLineOptionsProcessor.getString("sourceRegionName");
        String sourceProfile = commandLineOptionsProcessor.getString("sourceProfile");
        String destinationRegionName = commandLineOptionsProcessor.getString("destinationRegionName");
        String destinationProfile = commandLineOptionsProcessor.getString("destinationProfile");

        truncateDomainIfRequired(domainName);

        if (commandLineOptionsProcessor.hasParam("startYear")) {
            int startYear = commandLineOptionsProcessor.getInt("startYear");
            int endYear = commandLineOptionsProcessor.getInt("endYear");
            for (int timeFilter = startYear ; timeFilter <= endYear; timeFilter++) {
                _migrate(domainName, sourceRegionName, sourceProfile, destinationRegionName, destinationProfile, String.valueOf(timeFilter));
            }
        } else {
            String timeFilter = commandLineOptionsProcessor.getString("timeFilter");
            _migrate(domainName, sourceRegionName, sourceProfile, destinationRegionName, destinationProfile, timeFilter);
        }
    }

    private void truncateDomainIfRequired(String domainName) {
        boolean truncate = commandLineOptionsProcessor.getBoolean("truncate");
        if (truncate) {
            System.out.println("Deleting domain in destination - "+domainName);
            AmazonSimpleDBClient connection = getDestinationConnection();
            DeleteDomainRequest request = new DeleteDomainRequest(domainName);
            connection.deleteDomain(request);
        }
    }

    private void _migrate(String domainName, String sourceRegionName, String sourceProfile, String destinationRegionName, String destinationProfile, String year) throws IOException {
        SimpleDBExporter exporter =
                new SimpleDBExporter(sourceRegionName, domainName, sourceProfile, year);
        String fileName = exporter.export();
        SimpleDBImporter importer =
                new SimpleDBImporter(destinationRegionName, domainName, fileName, destinationProfile);
        importer.importDB();
    }

}
