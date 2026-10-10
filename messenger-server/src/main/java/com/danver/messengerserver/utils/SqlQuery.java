package com.danver.messengerserver.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;

public class SqlQuery {

    public SqlQuery(String query, Object ...args) throws JsonProcessingException {
        int paramsCount = StringUtils.countOccurrencesOf(query, "$");
        for (int i=0; i < args.length; i++) {
            if(args[i] instanceof Short) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::smallint");
            } else if(args[i] instanceof Integer) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::integer");
            } else if (args[i] instanceof Long) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::bigint");
            } else if (args[i] instanceof Float) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::float");
            } else if (args[i] instanceof Double) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::double precision");
            } else if (args[i] instanceof Character) {
                query = query.replaceAll("\\$" + (i+1), args[i] + "::text");
            } else if (args[i] instanceof String) {
                query = query.replaceAll("\\$" + (i+1), "'" + args[i] + "'" + "::text");
            } else if (args[i] instanceof Instant) {
                OffsetDateTime dateTime = args[i] == null ? null : ((Instant)args[i]).atOffset(ZoneOffset.UTC);
                String dateTimeString = dateTime == null ? null : dateTime.toString();
                query = query.replaceAll("\\$" + (i+1),  dateTimeString  + "::timestamp");
            } else if (args[i] instanceof HashMap) {
                ObjectMapper objectMapper = new ObjectMapper();
                String jacksonData = objectMapper.writeValueAsString(args[i]);
                query = query.replaceAll("\\$" + (i+1), jacksonData + "::jsonb");
            } else if (args[i] instanceof String[] || args[i] instanceof ArrayList<?>) {
                String arr = args[i].toString();
                query = query.replaceAll("\\$" + (i+1), arr + "::text[]");
            }
        }
    }
}
