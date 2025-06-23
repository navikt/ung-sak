package no.nav.ung.kodeverk.ytelse;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;

@Converter(autoApply = true)
public class KorrigertYtelseÅrsakKodeverdiConverter implements AttributeConverter<KorrigertYtelseÅrsak, String> {

    @Override
    public String convertToDatabaseColumn(KorrigertYtelseÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KorrigertYtelseÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KorrigertYtelseÅrsak.fraKode(dbData);
    }
}
