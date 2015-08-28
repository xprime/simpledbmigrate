#!/bin/bash

java -jar build/jar/simpledbmigrate.jar \
    --sourceRegionName ap-northeast-1 \
    --destinationRegionName ap-southeast-2 \
    --destinationProfile fxlmain \
    --timeFilter 2015 \
    --truncate false


