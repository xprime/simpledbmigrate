package com.feedxl.tools.simpledb;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.simpledb.AmazonSimpleDBAsyncClient;
import com.amazonaws.services.simpledb.model.*;
import com.feedxl.tools.aws.SimpleDBConnectionHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by vikram on 26/08/15.
 */
public class SimpleDBImporter {

    public static final int BATCH_LIMIT = 25;
    public static final int IN_QUEUE_LIMIT = 250;
    private final String regionName;
    private final String domainName;
    private final String fileName;
    private BufferedReader reader;
    private String profileName;
    private AmazonSimpleDBAsyncClient connection;
    private int total = 0;
    private int completed = 0;
    private int inQueue = 0;
    private int failed = 0;
    private int threads = 0;

    public SimpleDBImporter(String regionName, String domainName, String fileName, String profileName) {
        this.regionName = regionName;
        this.domainName = domainName;
        this.fileName = fileName;
        this.profileName = profileName;
    }

    public void importDB() throws IOException {
        initializeInputFile();
        initializeConnection();
        createDomainIfRequired();
        if (total > 0)
            batchUpdateDB();
        log(true);
        while (threads != 0) {
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDomainIfRequired() {
        ListDomainsRequest listDomainRequest = new ListDomainsRequest();
        ListDomainsResult listDomainsResult = connection.listDomains(listDomainRequest);
        List<String> domainNames = listDomainsResult.getDomainNames();
        if (!domainNames.contains(domainName)) {
            CreateDomainRequest createDomainRequest = new CreateDomainRequest(domainName);
            connection.createDomain(createDomainRequest);
        }
    }

    private void batchUpdateDB() throws IOException {
        if (inQueue > IN_QUEUE_LIMIT)
            return;
        List<ReplaceableItem> items;
        do {
            items = generateItems();
            BatchPutAttributesRequest request = new BatchPutAttributesRequest(domainName, items);
            inQueue += items.size();
            try {
                upload(request);
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
                inQueue -= items.size();
                failed += items.size();
            }
            if (inQueue > IN_QUEUE_LIMIT) {
                break;
            }
        } while (items.size() == BATCH_LIMIT);

        if (isAllItemsUploaded())
            reader.close();
    }

    private boolean isAllItemsUploaded() {
        return getUploadedItemsCount() == total;
    }

    private void upload(final BatchPutAttributesRequest request) {
        threads++;
        connection.batchPutAttributesAsync(request, new AsyncHandler<BatchPutAttributesRequest, Void>() {
            @Override
            public void onError(Exception exception) {
                System.out.println("Error: " + exception.getLocalizedMessage());
                int size = request.getItems().size();
                inQueue -= size;
                failed += size;
                threads--;
                log(false);
                restartBatchUpdateIfRequired();
            }

            @Override
            public void onSuccess(BatchPutAttributesRequest request, Void aVoid) {
                int size = request.getItems().size();
                completed += size;
                inQueue -= size;
                threads--;
                log(false);
                restartBatchUpdateIfRequired();
            }
        });
    }

    private void restartBatchUpdateIfRequired() {
        if (hasItemsToUpload())
            try {
                batchUpdateDB();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else {
            log(true);
            if (threads == 0) {
//                System.exit(0);
            }
        }
    }

    private boolean hasItemsToUpload() {
        return getUploadedItemsCount() < total;
    }

    private int getUploadedItemsCount() {
        return inQueue + failed + completed;
    }

    private void log(boolean force) {
        double completionPercent = (completed / (double) total) * 100.0d;
        double relativeCompletionToQuarter = completionPercent % 5.0d;
        if ((relativeCompletionToQuarter >= 0 && relativeCompletionToQuarter < 0.1) || force) {
            DecimalFormat df = new DecimalFormat();
            df.setMinimumFractionDigits(0);
            df.setMaximumFractionDigits(0);
            System.out.println("Uploaded - " + threads + "/" + completed + "/" + inQueue + "/" + failed + "/" + total + " - " + df.format(completionPercent) + "%");
        }
    }

    private void initializeConnection() {
        connection = new SimpleDBConnectionHelper().createSimpleDBAsyncConnection(profileName, regionName);
    }

    private int getLineCount() throws IOException {
        LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(fileName)));
        lnr.skip(Long.MAX_VALUE);
        lnr.close();
        return lnr.getLineNumber();
    }

    private List<ReplaceableItem> generateItems() throws IOException {
        List<ReplaceableItem> items = new ArrayList<ReplaceableItem>();
        String line;
        Gson gson = new GsonBuilder().create();
        while((line = reader.readLine()) != null) {
            Map itemInfo = gson.fromJson(line, Map.class);
            ReplaceableItem item = getReplaceableItem(itemInfo);
            items.add(item);
            if (items.size() == BATCH_LIMIT) {
                break;
            }
        }
        return items;
    }

    private ReplaceableItem getReplaceableItem(Map itemInfo) {
        String name = (String) itemInfo.get("itemName");
        ReplaceableItem item = new ReplaceableItem();
        item.setName(name);
        Collection<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
        List rawAttributes = (List) itemInfo.get("attributes");
        for (Object rAttr : rawAttributes) {
            Map attr = (Map) rAttr;
            ReplaceableAttribute replaceableAttribute = new ReplaceableAttribute((String)attr.get("name"),(String)attr.get("value"),true);
            replaceableAttributes.add(replaceableAttribute);
        }
        item.setAttributes(replaceableAttributes);
        return item;
    }

    private void initializeInputFile() throws IOException {
        System.out.println("Importing from file - " + fileName);
        File file = new File(fileName);
        FileReader fileReader = new FileReader(file);
        reader = new BufferedReader(fileReader);
        total = getLineCount();
    }

    public static void main(String [] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Missing Arguments: [region-name] [domain-name] [file] [profile]");
            return;
        }

        String regionName = args[0];
        String domain = args[1];
        String fileName = args[2];
        String profileName = args[3];

        SimpleDBImporter simpleDBExporter = new SimpleDBImporter(regionName, domain, fileName, profileName);
        simpleDBExporter.importDB();
    }
}
