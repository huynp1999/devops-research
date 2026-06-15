package com.demo.service;

import com.demo.repository.UserRepository;
import com.demo.util.QueryHelper;
import com.demo.util.FileUtil;
import com.demo.cache.ResultCache;

/**
 * File 2/6 - Service layer
 * Taint passes through method calls to repository and utilities
 */
public class UserService {

    private final UserRepository repository;
    private final ResultCache cache;

    public UserService(UserRepository repository, ResultCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public String findUser(String userId) {
        // Check cache first - taint flows into cache key
        String cached = cache.get("user_" + userId);
        if (cached != null) {
            return cached;
        }

        // Taint flows to repository via QueryHelper
        String condition = QueryHelper.buildCondition("id", userId);
        String result = repository.executeQuery("users", condition);

        cache.put("user_" + userId, result);
        return result;
    }

    public String generateReport(String reportName, String authToken) {
        // Bug 1: taint flows to file path (path traversal)
        String content = FileUtil.readFile("/opt/reports/" + reportName + ".pdf");

        // Bug 2: resource leak handled in FileUtil (see that file)
        return content;
    }
}
