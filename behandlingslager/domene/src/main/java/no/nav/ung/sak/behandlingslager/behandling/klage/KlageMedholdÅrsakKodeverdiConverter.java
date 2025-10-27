package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;

@Converter(autoApply = true)
public class KlageMedholdÅrsakKodeverdiConverter implements AttributeConverter<KlageMedholdÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(KlageMedholdÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KlageMedholdÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KlageMedholdÅrsak.fraKode(dbData);
    }
}
