package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.person.NavBrukerKjønn;

@Converter(autoApply = true)
public class KjønnKodeverdiConverter implements AttributeConverter<NavBrukerKjønn, String> {
    @Override
    public String convertToDatabaseColumn(NavBrukerKjønn attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public NavBrukerKjønn convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NavBrukerKjønn.fraKode(dbData);
    }
}
