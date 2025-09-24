package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

@Converter(autoApply = true)
public class KontrollertInntektKildeKodeverdiConverter implements AttributeConverter<KontrollertInntektKilde, String> {

    @Override
    public String convertToDatabaseColumn(KontrollertInntektKilde attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KontrollertInntektKilde convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KontrollertInntektKilde.fraKode(dbData);
    }
}
