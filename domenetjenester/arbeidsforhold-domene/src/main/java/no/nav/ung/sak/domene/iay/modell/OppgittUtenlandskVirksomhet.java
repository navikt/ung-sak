package no.nav.ung.sak.domene.iay.modell;

import java.io.Serializable;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;

/**
 * Hibernate entitet som modellerer en utenlandsk virksomhet.
 */
public class OppgittUtenlandskVirksomhet implements IndexKey, Serializable {


    private String utenlandskVirksomhetNavn;

    OppgittUtenlandskVirksomhet() {
    }

    public OppgittUtenlandskVirksomhet(String utenlandskVirksomhetNavn) {
        this.utenlandskVirksomhetNavn = utenlandskVirksomhetNavn;
    }

    /** deep copy ctor. */
    OppgittUtenlandskVirksomhet(OppgittUtenlandskVirksomhet kopierFra) {
        this.utenlandskVirksomhetNavn =kopierFra.utenlandskVirksomhetNavn;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { utenlandskVirksomhetNavn };
        return IndexKeyComposer.createKey(keyParts);
    }

    public String getNavn() {
        return utenlandskVirksomhetNavn;
    }
}
