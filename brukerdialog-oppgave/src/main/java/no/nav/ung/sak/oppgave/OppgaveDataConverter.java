package no.nav.ung.sak.oppgave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.sak.felles.tid.JsonObjectMapper;

@Converter
public class OppgaveDataConverter implements AttributeConverter<OppgaveData, String> {

    private static final ObjectMapper OBJECT_MAPPER = JsonObjectMapper.OM;

    @Override
    public String convertToDatabaseColumn(OppgaveData attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke serialisere OppgaveData til JSON", e);
        }
    }

    @Override
    public OppgaveData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, OppgaveData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke deserialisere JSON til OppgaveData", e);
        }
    }
}

