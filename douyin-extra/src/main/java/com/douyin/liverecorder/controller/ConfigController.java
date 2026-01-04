package com.douyin.liverecorder.controller;

import com.douyin.liverecorder.infrastructure.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Runtime configuration endpoints.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private final HttpClientUtil httpClientUtil;

    public ConfigController(HttpClientUtil httpClientUtil) {
        this.httpClientUtil = httpClientUtil;
    }

    public static class CookieUpdateRequest {
        private String cookie;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }
    }

    @PostMapping("/cookie")
    public ResponseEntity<Void> updateCookie(@RequestBody CookieUpdateRequest request) {
        if (request == null || request.getCookie() == null || request.getCookie().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        httpClientUtil.updateRuntimeCookie(request.getCookie());
        logger.info("Runtime cookie updated");
        return ResponseEntity.noContent().build();
    }
}
