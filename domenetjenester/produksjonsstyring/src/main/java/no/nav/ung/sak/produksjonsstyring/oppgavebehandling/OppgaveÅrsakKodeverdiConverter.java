package no.nav.ung.sak.produksjonsstyring.oppgavebehandling;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;

@Converter(autoApply = true)
public class OppgaveÅrsakKodeverdiConverter implements AttributeConverter<OppgaveÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(OppgaveÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public OppgaveÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OppgaveÅrsak.fraKode(dbData);
    }
}
