package com.danver.messengerserver.configs;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.MapperFeature;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.changeDefaultPropertyInclusion(incl ->
                incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                // Deserialization features (Jackson 3)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)

                .findAndAddModules();

//        (JsonInclude.Include.NON_NULL)
//                .featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
//                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        //.serializers(LOCAL_DATETIME_SERIALIZER);
    }
}
