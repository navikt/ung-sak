package no.nav.ung.sak.oppgave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;

@Converter
public class OppgaveDataConverter implements AttributeConverter<OppgavetypeDataDTO, String> {

    private static final ObjectMapper OBJECT_MAPPER = JsonObjectMapper.OM;

    @Override
    public String convertToDatabaseColumn(OppgavetypeDataDTO attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke serialisere OppgavetypeDataDTO til JSON", e);
        }
    }

    @Override
    public OppgavetypeDataDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, OppgavetypeDataDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke deserialisere JSON til OppgavetypeDataDTO", e);
        }
    }
}

