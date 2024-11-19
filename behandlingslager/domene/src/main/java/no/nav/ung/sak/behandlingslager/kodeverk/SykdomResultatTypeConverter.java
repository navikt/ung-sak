package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import no.nav.ung.kodeverk.sykdom.Resultat;

public class SykdomResultatTypeConverter implements AttributeConverter<Resultat, String> {
    @Override
    public String convertToDatabaseColumn(Resultat resultat) {
        return resultat.getKode();
    }

    @Override
    public Resultat convertToEntityAttribute(String kode) {
        for (Resultat resultat : Resultat.values()) {
            if (resultat.getKode().equals(kode)) {
                return resultat;
            }
        }
        return null;
    }
}
