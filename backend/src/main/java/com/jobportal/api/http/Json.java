package com.jobportal.api.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** The one configured Jackson ObjectMapper (thread-safe once configured). */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())                         // LocalDate / LocalDateTime support
            .registerModule(new Jdk8Module())                             // Optional -> value or null
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)      // "2026-10-03T11:00:00", not [2026,10,3,...]
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)   // tolerate extra fields from clients
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);     // omit null fields

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
