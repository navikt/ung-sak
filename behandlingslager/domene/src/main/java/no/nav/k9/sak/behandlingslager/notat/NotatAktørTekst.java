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

@Entity(name = "NotatAktørTekst")
@Table(name = "notat_aktoer_tekst")
public class NotatAktørTekst extends BaseEntitet implements NotatTekst {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_aktoer_tekst")
    private Long id;

    @OneToOne
    @JoinColumn(name = "notat_id")
    private NotatAktørEntitet notatAktørEntitet;

    @Column(name = "tekst")
    private String tekst;


    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public NotatAktørTekst(String tekst) {
        this.tekst = tekst;
    }

    public String getTekst() {
        return tekst;
    }
}
