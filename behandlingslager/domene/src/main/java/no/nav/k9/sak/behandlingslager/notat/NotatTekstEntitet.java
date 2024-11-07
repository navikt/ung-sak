package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import no.nav.k9.sak.behandlingslager.BaseEntitet;


/**
 * Representasjon av notattekst. Lagret tekst skal ikke endres.
 * Endring skal gi ny instans og versjon fordi alle tekstendringer må lagres for å støtte innsynskrav.
 *
 * Delt opp i 2 tabeller lagre sakspesifikke og personspesfikke tekster i hver for seg.
 * Dette pga etterlevelseskrav: Hvis noen ønsker å slette all data om seg selv så må vi kunne fjerne de på en enkel måte.
 *
 */
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

    public boolean erEndret() {
        return versjon > 0;
    }
}
