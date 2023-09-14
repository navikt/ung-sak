package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "NotatSakTekst")
@Table(name = "notat_sak_tekst")
public class NotatSakTekst extends BaseEntitet implements NotatTekst {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_sak_tekst")
    private Long id;

    @Column(name = "tekst", nullable = false, updatable = false)
    private String tekst;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    //Håndterer versjoner manuelt uten @Version fordi versjon er knyttet til fremmednøkkel notat_id og for
    // alle tekstendringer så lages ny rad.
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public NotatSakTekst(String tekst, long versjon) {
        this.tekst = tekst;
        this.versjon = versjon;
    }

    NotatSakTekst() { }

    public String getTekst() {
        return tekst;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean isAktiv() {
        return aktiv;
    }

    public long getVersjon() {
        return this.versjon;
    }
}
