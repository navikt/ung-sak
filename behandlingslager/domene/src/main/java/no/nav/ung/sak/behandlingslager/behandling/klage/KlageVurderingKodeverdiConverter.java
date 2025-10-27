package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.klage.KlageVurderingType;

@Converter(autoApply = true)
public class KlageVurderingKodeverdiConverter implements AttributeConverter<KlageVurderingType, String> {
    @Override
    public String convertToDatabaseColumn(KlageVurderingType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KlageVurderingType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KlageVurderingType.fraKode(dbData);
    }
}
