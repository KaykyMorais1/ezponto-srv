package com.ezponto.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());

        return new Jackson2ObjectMapperBuilder()
                .modules(new JavaTimeModule(), module)
                .featuresToDisable(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                );
    }

    static class OffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {

        OffsetDateTimeDeserializer() {
            super(OffsetDateTime.class);
        }

        @Override
        public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String value = p.getText().trim();
            try {
                return OffsetDateTime.parse(value);
            } catch (DateTimeParseException e) {
                // Aceita "yyyy-MM-dd" — interpreta como início do dia em UTC
                return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
            }
        }
    }
}
