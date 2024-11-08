package no.nav.k9.sak.behandlingslager.behandling.søknad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InnsendingsvalgKodeverdiConverter implements AttributeConverter<Innsendingsvalg, String> {
    @Override
    public String convertToDatabaseColumn(Innsendingsvalg attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Innsendingsvalg convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Innsendingsvalg.fraKode(dbData);
    }
}
