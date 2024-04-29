package no.nav.k9.sak.behandlingslager.notat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


/**
 * Notat som gjelder og lever sammen med en fagsak.
 */
@Entity(name = "NotatSakEntitet")
@Table(name = "notat_sak")
public class NotatSakEntitet extends NotatEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_sak")
    private Long id;

    @Column(name = "fagsak_id", updatable = false, nullable = false)
    private long fagsakId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "notat_id", nullable = false)
    @Where(clause = "aktiv = true")
    private List<NotatSakTekst> notatSakTekst;

    NotatSakEntitet(long fagsakId, String notatTekst, boolean skjult) {
        super(skjult);
        this.notatSakTekst = new ArrayList<>();
        this.notatSakTekst.add(new NotatSakTekst(notatTekst, 0));
        this.fagsakId = fagsakId;
    }

    NotatSakEntitet() {
    }

    public long getFagsakId() {
        return fagsakId;
    }

    @Override
    List<NotatSakTekst> getNotatTekstEntiteter() {
        return notatSakTekst;
    }

    @Override
    public void nyTekst(String tekst) {
        var notatSakTekst = this.finnNotatTekst();
        notatSakTekst.deaktiver();
        this.notatSakTekst.add(new NotatSakTekst(tekst, notatSakTekst.getVersjon() + 1));
    }
}
