package com.hardcraft.core.api;

public interface CoreApi {
    int CONTRACT_VERSION = 1;

    int contractVersion();

    ServiceRegistry services();
}