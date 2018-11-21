package com.feedxl.tools.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpledb.AmazonSimpleDBAsyncClient;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

public class SimpleDBConnectionHelper {

    public AmazonSimpleDBClient createSimpleDBConnection(String awsProfileName, String awsRegionName) {
        AWSCredentials credentials = getAwsCredentials(awsProfileName);
        AmazonSimpleDBClient simpleDBClient = new AmazonSimpleDBClient(credentials);
        setRegion(simpleDBClient, awsRegionName);
        return simpleDBClient;
    }

    public AmazonSimpleDBAsyncClient createSimpleDBAsyncConnection(String awsProfileName, String awsRegionName) {
        AWSCredentials credentials = getAwsCredentials(awsProfileName);
        AmazonSimpleDBAsyncClient simpleDBClient = new AmazonSimpleDBAsyncClient(credentials);
        setRegion(simpleDBClient, awsRegionName);
        return simpleDBClient;
    }

    private void setRegion(AmazonSimpleDBClient simpleDBClient, String awsRegionName) {
        Region region = Region.getRegion(Regions.fromName(awsRegionName));
        simpleDBClient.setRegion(region);
    }

    private AWSCredentials getAwsCredentials(String awsProfileName) {
        return new ProfileCredentialsProvider(awsProfileName).getCredentials();
    }
}