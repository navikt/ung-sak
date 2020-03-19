package no.nav.k9.sak.domene.iay.modell;

import java.io.Serializable;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.geografisk.Landkoder;

/**
 * Hibernate entitet som modellerer en utenlandsk virksomhet.
 */
public class OppgittUtenlandskVirksomhet implements IndexKey, Serializable {

    private Landkoder landkode = Landkoder.NOR;

    private String utenlandskVirksomhetNavn;

    OppgittUtenlandskVirksomhet() {
    }

    public OppgittUtenlandskVirksomhet(Landkoder landkode, String utenlandskVirksomhetNavn) {
        this.landkode = landkode == null? Landkoder.NOR : landkode;
        this.utenlandskVirksomhetNavn = utenlandskVirksomhetNavn;
    }
    @Override
    public String getIndexKey() {
        Object[] keyParts = { utenlandskVirksomhetNavn, landkode };
        return IndexKeyComposer.createKey(keyParts);
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public String getNavn() {
        return utenlandskVirksomhetNavn;
    }
}
