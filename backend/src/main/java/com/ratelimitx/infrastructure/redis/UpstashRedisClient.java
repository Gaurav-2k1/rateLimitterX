package com.ratelimitx.infrastructure.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class UpstashRedisClient {

    @Value("${upstash.redis.url}")
    private String redisUrl;

    @Value("${upstash.redis.token}")
    private String redisToken;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UpstashRedisClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private String executeCommand(String... commands) {
        try {
            String jsonBody = objectMapper.writeValueAsString(java.util.Arrays.asList(commands));

            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(redisUrl)
                    .addHeader("Authorization", "Bearer " + redisToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Redis command failed: {} - {}", response.code(), response.message());
                    throw new RuntimeException("Redis operation failed: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                if (jsonNode.has("result")) {
                    JsonNode resultNode = jsonNode.get("result");
                    if (resultNode.isNull()) {
                        return null;
                    }
                    if (resultNode.isTextual()) {
                        return resultNode.asText();
                    }
                    return resultNode.toString();
                }
                return responseBody;
            }
        } catch (IOException e) {
            log.error("Error executing Redis command", e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    /**
     * Execute a Lua script atomically
     * @param script The Lua script to execute
     * @param keys Array of Redis keys (KEYS in Lua)
     * @param args Array of arguments (ARGV in Lua)
     * @return The result from the script
     */
    public String eval(String script, String[] keys, String... args) {
        try {
            // Build EVAL command: EVAL script numkeys key [key ...] arg [arg ...]
            int numKeys = keys != null ? keys.length : 0;
            int numArgs = args != null ? args.length : 0;
            int totalSize = 3 + numKeys + numArgs; // EVAL + script + numkeys + keys + args

            String[] command = new String[totalSize];
            command[0] = "EVAL";
            command[1] = script;
            command[2] = String.valueOf(numKeys);

            int idx = 3;
            if (keys != null) {
                for (String key : keys) {
                    command[idx++] = key;
                }
            }
            if (args != null) {
                for (String arg : args) {
                    command[idx++] = arg;
                }
            }

            return executeCommand(command);

        } catch (Exception e) {
            log.error("Error executing Lua script", e);
            throw new RuntimeException("Lua script execution failed", e);
        }
    }

    public long incr(String key) {
        String result = executeCommand("INCR", key);
        return Long.parseLong(result);
    }

    public void expire(String key, int seconds) {
        executeCommand("EXPIRE", key, String.valueOf(seconds));
    }

    public void set(String key, String value) {
        executeCommand("SET", key, value);
    }

    public String get(String key) {
        return executeCommand("GET", key);
    }

    public void hset(String key, String field, String value) {
        executeCommand("HSET", key, field, value);
    }

    public String hget(String key, String field) {
        String result = executeCommand("HGET", key, field);
        return result != null && !result.equals("null") ? result : null;
    }

    public void hmset(String key, String... fieldValues) {
        if (fieldValues.length % 2 != 0) {
            throw new IllegalArgumentException("Field-value pairs must be even");
        }
        String[] command = new String[fieldValues.length + 2];
        command[0] = "HMSET";
        command[1] = key;
        System.arraycopy(fieldValues, 0, command, 2, fieldValues.length);
        executeCommand(command);
    }

    public long zadd(String key, long score, String member) {
        String result = executeCommand("ZADD", key, String.valueOf(score), member);
        return Long.parseLong(result);
    }

    public long zremrangebyscore(String key, long min, long max) {
        String result = executeCommand("ZREMRANGEBYSCORE", key, String.valueOf(min), String.valueOf(max));
        return Long.parseLong(result);
    }

    public long zcard(String key) {
        String result = executeCommand("ZCARD", key);
        return Long.parseLong(result);
    }

    public void del(String key) {
        executeCommand("DEL", key);
    }
}