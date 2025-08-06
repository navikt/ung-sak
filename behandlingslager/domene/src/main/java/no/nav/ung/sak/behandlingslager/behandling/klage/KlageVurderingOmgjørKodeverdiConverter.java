package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;

@Converter(autoApply = true)
public class KlageVurderingOmgjørKodeverdiConverter implements AttributeConverter<KlageVurderingOmgjør, String> {
    @Override
    public String convertToDatabaseColumn(KlageVurderingOmgjør attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KlageVurderingOmgjør convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KlageVurderingOmgjør.fraKode(dbData);
    }
}
