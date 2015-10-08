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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleDBExporter {

    public static final int LIMIT = 2500;
    
    private String regionName;
    private String domainName;
    private BufferedWriter writer;
    private String profile;
    private String timeFilter;
    private String outputFileName;
	private CommandLineOptionsProcessor commandLineOptionsProcessor;

    public SimpleDBExporter(String [] args) {
    	commandLineOptionsProcessor = new CommandLineOptionsProcessor(args);
        if (!commandLineOptionsProcessor.processInput()) {
            System.exit(-1);
        }
        if (!commandLineOptionsProcessor.validateMandatoryFields("domainName", "regionName", "profile")) {
            System.exit(-1);
        }
        commandLineOptionsProcessor.populateDefaultsIfMissing("timeFilter", "");
        
        this.regionName = commandLineOptionsProcessor.getString("regionName");
        this.domainName = commandLineOptionsProcessor.getString("domainName");
        this.profile = commandLineOptionsProcessor.getString("profile");
        this.timeFilter = commandLineOptionsProcessor.getString("timeFilter");        
	}

	public SimpleDBExporter(String regionName, String domainName, String profileName, String timeFilter) {
        this.regionName = regionName;
        this.domainName = domainName;
        this.profile = profileName;
        this.timeFilter = timeFilter;
    }
	
    public String export() throws IOException {
        initializeOutputFile();
        AmazonSimpleDBClient simpleDBClient = new SimpleDBConnectionHelper().createSimpleDBConnection(profile, regionName);
        SimpleDBSelectIterator iterator = new SimpleDBSelectIterator(simpleDBClient, domainName, LIMIT, timeFilter);
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
        outputFileName = domainName + ".sdb";
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
        private String domain;
        private int limit;
        private String timeFilter;
        private int total;
        private int downloaded;
        private String nextToken;

        public SimpleDBSelectIterator(AmazonSimpleDBClient simpleDBClient, String domain, int limit, final String timeFilter) {
            this.simpleDBClient = simpleDBClient;
            this.domain = domain;
            this.limit = limit;
            this.timeFilter = timeFilter;
            String selectExpression = "select count(*) from " + domain;
            if (!timeFilter.equals(""))
                selectExpression += " where time like '" + timeFilter + "%'";
            SelectRequest selectRequest = new SelectRequest(selectExpression);
            SelectResult result = simpleDBClient.select(selectRequest);
            List<Item> items = result.getItems();
            List<Attribute> attributes = items.get(0).getAttributes();
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals("Count")) {
                    total = Integer.parseInt(attribute.getValue());
                    System.out.println("Total records - "+total);
                    break;
                }
            }
            downloaded = 0;
            log(true);
        }

        @Override
        public boolean hasNext() {
            return downloaded < total;
        }

        @Override
        public Object next() {
            String selectExpression = "select * from " + domain;
            if (!timeFilter.equals("")) {
                selectExpression += " where time like '" + timeFilter + "%'";
            }
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