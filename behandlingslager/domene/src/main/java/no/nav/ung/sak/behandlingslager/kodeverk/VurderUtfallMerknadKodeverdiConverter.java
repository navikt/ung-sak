package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.vilkår.VilkårUtfallMerknad;

@Converter(autoApply = true)
public class VurderUtfallMerknadKodeverdiConverter implements AttributeConverter<VilkårUtfallMerknad, String> {
    @Override
    public String convertToDatabaseColumn(VilkårUtfallMerknad attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public VilkårUtfallMerknad convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VilkårUtfallMerknad.fraKode(dbData);
    }
}
