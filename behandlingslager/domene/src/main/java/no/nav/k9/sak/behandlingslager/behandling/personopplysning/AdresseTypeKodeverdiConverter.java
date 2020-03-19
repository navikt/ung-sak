package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import java.util.List;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.geografisk.AdresseType;

@Converter(autoApply = true)
public class AdresseTypeKodeverdiConverter implements AttributeConverter<AdresseType, String> {
    @Override
    public String convertToDatabaseColumn(AdresseType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AdresseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdresseType.fraKode(dbData);
    }

    public static AdresseType finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(AdresseType.values()).stream().filter(k -> Objects.equals(k.getOffisiellKode(), offisiellDokumentType)).findFirst().orElse(AdresseType.UKJENT_ADRESSE);
    }
}