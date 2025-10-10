package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;

@Converter(autoApply = true)
public class KlageVurdertAvKodeverdiConverter implements AttributeConverter<KlageVurdertAv, String> {
    @Override
    public String convertToDatabaseColumn(KlageVurdertAv attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KlageVurdertAv convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KlageVurdertAv.fraKode(dbData);
    }
}
