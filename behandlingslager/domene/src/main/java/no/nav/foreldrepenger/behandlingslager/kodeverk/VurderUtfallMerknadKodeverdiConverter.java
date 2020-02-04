package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;

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