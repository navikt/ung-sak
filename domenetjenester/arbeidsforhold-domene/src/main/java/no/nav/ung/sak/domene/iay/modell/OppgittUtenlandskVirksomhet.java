package no.nav.ung.sak.domene.iay.modell;

import java.io.Serializable;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;

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

    /** deep copy ctor. */
    OppgittUtenlandskVirksomhet(OppgittUtenlandskVirksomhet kopierFra) {
        this.landkode=kopierFra.landkode;
        this.utenlandskVirksomhetNavn =kopierFra.utenlandskVirksomhetNavn;
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
