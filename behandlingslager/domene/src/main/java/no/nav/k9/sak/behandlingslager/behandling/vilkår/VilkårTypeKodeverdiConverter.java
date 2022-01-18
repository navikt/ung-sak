package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.vilkår.VilkårType;

@Converter(autoApply = true)
public class VilkårTypeKodeverdiConverter implements AttributeConverter<VilkårType, String> {
    @Override
    public String convertToDatabaseColumn(VilkårType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public VilkårType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VilkårType.fraKode(dbData);
    }
}
