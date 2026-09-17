package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Immutable
@Entity(name = "OppgittForutgåendeMedlemskapPeriode")
@Table(name = "OPPGITT_FMEDLEMSKAP")
public class OppgittForutgåendeMedlemskapPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPGITT_FMEDLEMSKAP")
    private Long id;

    @Column(name = "journalpost_id", nullable = false, updatable = false)
    private String journalpostId;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "har_bodd_i_norge", nullable = false, updatable = false)
    private boolean harBoddINorge;

    @Column(name = "har_jobbet_i_norge", updatable = false)
    private Boolean harJobbetINorge;

    @Column(name = "har_jobbet_utenfor_norge", updatable = false)
    private Boolean harJobbetUtenforNorge;

    @BatchSize(size = 20)
    @JoinColumn(name = "oppgitt_fmedlemskap_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OppgittUtenlandsopphold> utenlandsopphold = new LinkedHashSet<>();

    public static Builder builder() {
        return new Builder();}

    public OppgittForutgåendeMedlemskapPeriode() {
    }

    private OppgittForutgåendeMedlemskapPeriode(JournalpostId journalpostId, LocalDate fom, LocalDate tom, Set<OppgittUtenlandsopphold> utenlandsopphold, boolean harBoddINorge, Boolean harJobbetINorge, Boolean harJobbetUtenforNorge) {
        Objects.requireNonNull(journalpostId, "journalpostId");
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");

        valider(utenlandsopphold, harBoddINorge, harJobbetINorge, harJobbetUtenforNorge);

        this.journalpostId = journalpostId.getVerdi();
        this.periode = Range.closed(fom, tom);
        this.harBoddINorge = harBoddINorge;
        this.harJobbetINorge = harJobbetINorge;
        this.harJobbetUtenforNorge = harJobbetUtenforNorge;
        this.utenlandsopphold = utenlandsopphold != null ? new LinkedHashSet<>(utenlandsopphold) : new LinkedHashSet<>();
    }

    private static void valider(Set<OppgittUtenlandsopphold> utenlandsopphold, boolean harBoddINorge, Boolean harJobbetINorge, Boolean harJobbetUtenforNorge) {
        if (!harBoddINorge) {
            Objects.requireNonNull(harJobbetINorge, "harJobbetINorge må være satt hvis harBoddINorge er false");
        }

        if (harBoddINorge || harJobbetINorge) {
            Objects.requireNonNull(harJobbetUtenforNorge, "harJobbetUtenforNorge må være satt hvis harBoddINorge er true eller harJobbetINorge er true");
        }

        if (Boolean.TRUE.equals(harJobbetUtenforNorge) || Boolean.FALSE.equals(harJobbetINorge)) {
            Objects.requireNonNull(utenlandsopphold, "utenlandsopphold");
            if (utenlandsopphold.isEmpty()) {
                throw new IllegalArgumentException("utenlandsopphold kan ikke være tom hvis harJobbetUtenforNorge er true eller harJobbetINorge er false");
            }
        }
    }

    OppgittForutgåendeMedlemskapPeriode(OppgittForutgåendeMedlemskapPeriode other) {
        this.journalpostId = other.journalpostId;
        this.periode = other.periode;
        this.harBoddINorge = other.harBoddINorge;
        this.harJobbetINorge = other.harJobbetINorge;
        this.harJobbetUtenforNorge = other.harJobbetUtenforNorge;
        this.utenlandsopphold = other.utenlandsopphold.stream()
            .map(OppgittUtenlandsopphold::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public boolean harBoddINorge() {
        return harBoddINorge;
    }

    public Boolean harJobbetINorge() {
        return harJobbetINorge;
    }

    public Boolean harJobbetUtenforNorge() {
        return harJobbetUtenforNorge;
    }

    public Set<OppgittUtenlandsopphold> getUtenlandsopphold() {
        return Collections.unmodifiableSet(utenlandsopphold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittForutgåendeMedlemskapPeriode that = (OppgittForutgåendeMedlemskapPeriode) o;
        return harBoddINorge == that.harBoddINorge
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(getPeriode(), that.getPeriode())
            && Objects.equals(harJobbetINorge, that.harJobbetINorge)
            && Objects.equals(harJobbetUtenforNorge, that.harJobbetUtenforNorge)
            && Objects.equals(utenlandsopphold, that.utenlandsopphold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, getPeriode(), harBoddINorge, harJobbetINorge, harJobbetUtenforNorge, utenlandsopphold);
    }

    public static class Builder {
        private JournalpostId journalpostId;
        private LocalDate fom;
        private LocalDate tom;
        private Set<OppgittUtenlandsopphold> utenlandsopphold;
        private Boolean harBoddINorge;
        private Boolean harJobbetINorge;
        private Boolean harJobbetUtenforNorge;

        public Builder medJournalpostId(JournalpostId journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            this.tom = tom;
            return this;
        }

        public Builder medUtenlandsopphold(Set<OppgittUtenlandsopphold> utenlandsopphold) {
            this.utenlandsopphold = utenlandsopphold;
            return this;
        }

        public Builder medHarBoddINorge(boolean harBoddINorge) {
            this.harBoddINorge = harBoddINorge;
            return this;
        }

        public Builder medHarJobbetINorge(Boolean harJobbetINorge) {
            this.harJobbetINorge = harJobbetINorge;
            return this;
        }

        public Builder medHarJobbetUtenforNorge(Boolean harJobbetUtenforNorge) {
            this.harJobbetUtenforNorge = harJobbetUtenforNorge;
            return this;
        }

        public OppgittForutgåendeMedlemskapPeriode build() {
            Objects.requireNonNull(harBoddINorge, "harBoddINorge");
            return new OppgittForutgåendeMedlemskapPeriode(journalpostId, fom, tom, utenlandsopphold, harBoddINorge, harJobbetINorge, harJobbetUtenforNorge);
        }
    }
}
