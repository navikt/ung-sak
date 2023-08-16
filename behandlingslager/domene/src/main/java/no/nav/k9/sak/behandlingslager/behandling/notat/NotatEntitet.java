package no.nav.k9.sak.behandlingslager.behandling.notat;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "NotatEntitet")
@Table(name = "notat")
public class NotatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_NOTAT")
    private Long id;

    @Column(name = "notat_tekst", nullable = false, updatable = false)
    private String notatTekst;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "gjelder_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId gjelder;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Column(name = "skjult", nullable = false, updatable = false)
    private boolean skjult = false;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Column(name = "erstattet_av_notat_id", nullable = true, updatable = false)
    private Long erstattetAvNotatId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NotatEntitet(Long id, String notatTekst, AktørId gjelder, Long fagsakId, boolean skjult) {
        this.id = id;
        this.notatTekst = notatTekst;
        this.gjelder = gjelder;
        this.fagsakId = fagsakId;
        this.skjult = skjult;
    }

    NotatEntitet() { }

    public Long getId() {
        return id;
    }

    public String getNotatTekst() {
        return notatTekst;
    }

    public AktørId getGjelder() {
        return gjelder;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public boolean isSkjult() {
        return skjult;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public Long getErstattetAvNotatId() {
        return erstattetAvNotatId;
    }
}
