package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "AvklartSøknadsfristResultat")
@Table(name = "RS_SOKNADSFRIST")
@Immutable
public class AvklartSøknadsfristResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_SOKNADSFRIST")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "overstyrt_id", updatable = false, unique = true)
    private KravDokumentHolder overstyrtHolder;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "avklart_id", updatable = false, unique = true)
    private KravDokumentHolder avklartHolder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    AvklartSøknadsfristResultat() {
        // hibernate
    }

    public AvklartSøknadsfristResultat(KravDokumentHolder overstyrtHolder, KravDokumentHolder avklartHolder) {
        this.overstyrtHolder = overstyrtHolder;
        this.avklartHolder = avklartHolder;
    }

    public KravDokumentHolder getOverstyrtHolder() {
        return overstyrtHolder;
    }

    public KravDokumentHolder getAvklartHolder() {
        return avklartHolder;
    }

    void setBehandlingId(Long behandlingId) {
        if (this.behandlingId != null) {
            throw new IllegalStateException("Forsøker å endre behandlingId på persistert grunnlag");
        }
        this.behandlingId = Objects.requireNonNull(behandlingId);
    }

    public Optional<AvklartKravDokument> finnAvklaring(JournalpostId journalpostId) {
        var overstyrtStatus = overstyrtHolder.getDokumenter()
            .stream()
            .filter(it -> it.getJournalpostId().equals(journalpostId))
            .findAny();

        if (overstyrtStatus.isPresent()) {
            return overstyrtStatus;
        }

        return avklartHolder.getDokumenter()
            .stream()
            .filter(it -> it.getJournalpostId().equals(journalpostId))
            .findAny();
    }

    void deaktiver() {
        this.aktiv = false;
    }
}
