package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

import no.nav.k9.kodeverk.medisinsk.Pleiegrad;

public class PleiegradTypeConverter implements AttributeConverter<Pleiegrad, String> {
    @Override
    public String convertToDatabaseColumn(Pleiegrad type) {
        return type.getKode();
    }

    @Override
    public Pleiegrad convertToEntityAttribute(String kode) {
        for (Pleiegrad type : Pleiegrad.values()) {
            if (type.getKode().equals(kode)) {
                return type;
            }
        }
        return null;
    }
}
