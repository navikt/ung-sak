package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.vedtak.VedtakResultatType;

@Converter(autoApply = true)
public class VedtakResultatTypeKodeverdiConverter implements AttributeConverter<VedtakResultatType, String> {
    @Override
    public String convertToDatabaseColumn(VedtakResultatType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public VedtakResultatType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VedtakResultatType.fraKode(dbData);
    }
}
