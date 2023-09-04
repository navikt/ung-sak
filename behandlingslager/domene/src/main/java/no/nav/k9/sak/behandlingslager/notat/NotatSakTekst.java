package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "NotatSakTekst")
@Table(name = "notat_sak_tekst")
public class NotatSakTekst extends BaseEntitet implements NotatTekst {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_sak_tekst")
    private Long id;

    @OneToOne
    @JoinColumn(name = "notat_id")
    private NotatSakEntitet notatSakEntitet;

    @Column(name = "tekst")
    private String tekst;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public NotatSakTekst(String tekst, NotatSakEntitet notatSakEntitet) {
        this.tekst = tekst;
        this.notatSakEntitet = notatSakEntitet;
    }

    public String getTekst() {
        return tekst;
    }
}
