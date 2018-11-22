package com.feedxl.tools.simpledb;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.feedxl.tools.aws.SimpleDBConnectionHelper;
import com.feedxl.tools.util.CommandLineOptionsProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class SimpleDBExporter {

    public static final int LIMIT = 2500;

    private String awsRegionName;
    private String sdbDomainName;
    private BufferedWriter writer;
    private String awsProfileName;
    private String timeFilter;
    private String nanoProfileName;
    private String outputFileName;
    private CommandLineOptionsProcessor commandLineOptionsProcessor;

    public SimpleDBExporter(String[] args) {
        commandLineOptionsProcessor = new CommandLineOptionsProcessor(args);
        if (!commandLineOptionsProcessor.processInput()) {
            System.exit(-1);
        }
        if (!commandLineOptionsProcessor.validateMandatoryFields("awsProfileName", "awsRegionName", "sdbDomainName")) {
            System.exit(-1);
        }
        commandLineOptionsProcessor.populateDefaultsIfMissing("timeFilter", "");
        commandLineOptionsProcessor.populateDefaultsIfMissing("nanoProfileName", "");

        this.awsProfileName = commandLineOptionsProcessor.getString("awsProfileName");
        this.awsRegionName = commandLineOptionsProcessor.getString("awsRegionName");
        this.sdbDomainName = commandLineOptionsProcessor.getString("sdbDomainName");
        this.timeFilter = commandLineOptionsProcessor.getString("timeFilter");
        this.nanoProfileName = commandLineOptionsProcessor.getString("nanoProfileName");
    }

    public SimpleDBExporter(String awsProfileName, String awsRegionName, String sdbDomainName, String timeFilter, String nanoProfileName) {
        this.awsProfileName = awsProfileName;
        this.awsRegionName = awsRegionName;
        this.sdbDomainName = sdbDomainName;
        this.timeFilter = timeFilter;
        this.nanoProfileName = nanoProfileName;
    }

    public String export() throws IOException {
        initializeOutputFile();
        AmazonSimpleDBClient simpleDBClient = new SimpleDBConnectionHelper().createSimpleDBConnection(awsProfileName, awsRegionName);
        SimpleDBSelectIterator iterator = new SimpleDBSelectIterator(simpleDBClient, sdbDomainName, LIMIT, timeFilter, nanoProfileName);
        download(iterator);
        return outputFileName;
    }

    private void download(SimpleDBSelectIterator iterator) throws IOException {
        Gson gson = new GsonBuilder()
                .create();
        while (iterator.hasNext()) {
            List<Item> items = (List<Item>) iterator.next();
            for (Item item : items) {
                List<Attribute> attributes = item.getAttributes();
                Map serializableItem = new HashMap();
                serializableItem.put("itemName", item.getName());
                serializableItem.put("attributes", attributes);
                String attributesAsJson = gson.toJson(serializableItem);
                writer.write(attributesAsJson);
                writer.write("\n");
            }
        }
        writer.close();
        iterator.log(true);
    }

    private void initializeOutputFile() throws IOException {
        outputFileName = sdbDomainName + ".sdb";
        File file = new File(outputFileName);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        System.out.println("Saving to file - " + outputFileName);
        FileWriter fileWriter = new FileWriter(file);
        writer = new BufferedWriter(fileWriter);
    }

    private static class SimpleDBSelectIterator implements Iterator {
        private AmazonSimpleDBClient simpleDBClient;
        private String sdbDomain;
        private int limit;
        private String timeFilter;
        private int total;
        private int downloaded;
        private String nextToken;
        private String nanoProfileName;

        public SimpleDBSelectIterator(AmazonSimpleDBClient simpleDBClient, String sdbDomain, int limit, final String timeFilter, String nanoProfileName) {
            this.simpleDBClient = simpleDBClient;
            this.sdbDomain = sdbDomain;
            this.limit = limit;
            this.timeFilter = timeFilter;
            this.nanoProfileName = nanoProfileName;
            init(simpleDBClient);
            log(true);
        }

        private void init(AmazonSimpleDBClient simpleDBClient) {
            SelectRequest selectRequest = getRowCountSelectRequest();
            SelectResult result = simpleDBClient.select(selectRequest);
            List<Item> items = result.getItems();
            List<Attribute> attributes = items.get(0).getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals("Count")) {
                    total = Integer.parseInt(attribute.getValue());
                    System.out.println("Total records - " + total);
                    break;
                }
            }
            downloaded = 0;
        }

        private SelectRequest getRowCountSelectRequest() {
            return new SelectRequest(getSelectQuery("count(*)"));
        }

        private String getSelectQuery(final String columns) {
            String selectExpression = "select " + columns + " from " + this.sdbDomain;
            List<String> filters = new ArrayList<>();
            if (!this.timeFilter.equals(""))
                filters.add("time like '" + this.timeFilter + "%'");
            if (!this.nanoProfileName.equals(""))
                filters.add("profileName = '" + this.nanoProfileName + "'");
            if (filters.size() > 0)
                selectExpression += " where " + String.join(" and ", filters);
            System.out.println("SimpleDBExporter: " + selectExpression);
            return selectExpression;
        }

        @Override
        public boolean hasNext() {
            return downloaded < total;
        }

        @Override
        public Object next() {
            String selectExpression = getSelectQuery("*");
            selectExpression += " limit " + limit;
            SelectRequest selectRequest = new SelectRequest(selectExpression);
            if (nextToken != null)
                selectRequest.setNextToken(nextToken);
            SelectResult result = simpleDBClient.select(selectRequest);
            List<Item> items = result.getItems();
            downloaded += items.size();
            nextToken = result.getNextToken();
            log(false);
            return items;
        }

        @Override
        public void remove() {

        }

        private void log(boolean force) {
            double completionPercent = (downloaded / (double) total) * 100.0d;
            double relativeCompletionToQuarter = completionPercent % 25;
            if ((relativeCompletionToQuarter > 0 && relativeCompletionToQuarter < 5) || force) {
                DecimalFormat df = new DecimalFormat();
                df.setMinimumFractionDigits(0);
                df.setMaximumFractionDigits(0);
                System.out.println("Downloaded - " + downloaded + "/" + total + " - " + df.format(completionPercent) + "%");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new SimpleDBExporter(args).export();
    }
}