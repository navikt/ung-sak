package no.nav.k9.sak.behandlingslager.notat;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;


@Entity(name = "NotatSakEntitet")
@Table(name = "notat_sak")
public class NotatSakEntitet extends BaseEntitet implements Notat {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_sak")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "fagsak_id")
    private long fagsakId;


    @OneToOne(mappedBy = "notatSakEntitet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotatSakTekst notatSakTekst;


    @Column(name = "skjult", nullable = false, updatable = true)
    private boolean skjult;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NotatSakEntitet(long fagsakId, String notatTekst, boolean skjult) {
        this.notatSakTekst = new NotatSakTekst(notatTekst, this);
        this.fagsakId = fagsakId;
        this.skjult = skjult;
        this.uuid = UUID.randomUUID();
    }

    NotatSakEntitet() {
    }

    public long getFagsakId() {
        return fagsakId;
    }

    @Override
    public String getNotatTekst() {
        return notatSakTekst.getTekst();
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public long getVersjon() {
        return versjon;
    }

    public NotatGjelderType getGjelderType() {
        return NotatGjelderType.PLEIETRENGENDE;
    }

    @Override
    public Long getId() {
        return id;
    }

    public boolean isSkjult() {
        return skjult;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void skjul(boolean skjul) {
        this.skjult = skjul;
    }
}
