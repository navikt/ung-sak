package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "AvklartSøknadsfristResultat")
@Table(name = "RS_SOKNADSFRIST")
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

    public Optional<KravDokumentHolder> getOverstyrtHolder() {
        return Optional.ofNullable(overstyrtHolder);
    }

    public Optional<KravDokumentHolder> getAvklartHolder() {
        return Optional.ofNullable(avklartHolder);
    }

    void setBehandlingId(Long behandlingId) {
        if (this.behandlingId != null) {
            throw new IllegalStateException("Forsøker å endre behandlingId på persistert grunnlag");
        }
        this.behandlingId = Objects.requireNonNull(behandlingId);
    }

    public Optional<AvklartKravDokument> finnAvklaring(JournalpostId journalpostId) {

        if (overstyrtHolder != null) {
            var overstyrtStatus = overstyrtHolder.getDokumenter()
                .stream()
                .filter(it -> it.getJournalpostId().equals(journalpostId))
                .findAny();

            if (overstyrtStatus.isPresent()) {
                return overstyrtStatus;
            }
        }

        if (avklartHolder != null) {
            return avklartHolder.getDokumenter()
                .stream()
                .filter(it -> it.getJournalpostId().equals(journalpostId))
                .findAny();
        }
        return Optional.empty();
    }

    void deaktiver() {
        this.aktiv = false;
    }
}
