package no.nav.k9.sak.behandlingslager.behandling;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;

@Converter(autoApply = true)
public class KonsekvensForYtelsenKodeverdiConverter implements AttributeConverter<KonsekvensForYtelsen, String> {
    @Override
    public String convertToDatabaseColumn(KonsekvensForYtelsen attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KonsekvensForYtelsen convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KonsekvensForYtelsen.fraKode(dbData);
    }
}