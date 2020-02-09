package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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