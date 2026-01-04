package com.stetits.core.persistence;

import java.util.List;
import java.util.Optional;

public interface StackVersionsRepository {

    Optional<String> getBodyJson(String stackId, String version);

    List<String> listVersions(String stackId);

    void insert(StackVersionRow row);

    record StackVersionRow(
            String stackId,
            String version,
            String parentVersion,
            String bodyJson,
            String bodySha256,
            String createdBy,
            String comment
    ) {}
}
