package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import no.nav.k9.sak.behandlingslager.BaseEntitet;


@MappedSuperclass
abstract class NotatTekstEntitet extends BaseEntitet {

    @Column(name = "tekst", nullable = false, updatable = false)
    private String tekst;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    //Håndterer versjoner manuelt uten @Version fordi versjon er knyttet til fremmednøkkel notat_id og for
    // alle tekstendringer så lages ny rad.
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NotatTekstEntitet(String tekst, long versjon) {
        this.tekst = tekst;
        this.versjon = versjon;
    }


    NotatTekstEntitet() { }

    public String getTekst() {
        return tekst;
    }

    public void deaktiver() {
        aktiv = false;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public long getVersjon() {
        return versjon;
    }
}
