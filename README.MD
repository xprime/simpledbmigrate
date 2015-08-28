# SimpleDBMigrate

Operate on Amazon Web Services simpledb no-sql database
1. migrate from one account to another
2. backup into a file system
3. restore from backup

### Pre-requisites
1. Create aws credentials and profile files by using aws cli.
It usually goes into ~/.aws/credentials and ~/.aws/config. Be
careful about the format of these files as they differ slightly
and hence it is best advised to use aws cli tools and run
aws configure --profile <profile name>
2. Create a profile for each of the aws accounts. Atleast
one is required as the tool does not directly take AWS shared
key and secret.

### How to use
1. Build the jar - `ant jar`
2. Run as -
    ```
    java -jar build/jar/simpledbmigrate.jar \
    --sourceRegionName ap-northeast-1 \
    --sourceProfile src \
    --destinationRegionName ap-southeast-2 \
    --destinationProfile dest \
    --timeFilter 2015 \
    --truncate false \
    --startYear 2011 \
    --endYear 2012 \
    ```

