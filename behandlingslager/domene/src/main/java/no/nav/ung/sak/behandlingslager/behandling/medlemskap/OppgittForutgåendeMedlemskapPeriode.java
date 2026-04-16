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

    @BatchSize(size = 20)
    @JoinColumn(name = "oppgitt_fmedlemskap_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OppgittBosted> bostederUtland = new LinkedHashSet<>();

    public OppgittForutgåendeMedlemskapPeriode() {
    }

    public OppgittForutgåendeMedlemskapPeriode(JournalpostId journalpostId, LocalDate fom, LocalDate tom, Set<OppgittBosted> bosteder) {
        Objects.requireNonNull(journalpostId, "journalpostId");
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        this.journalpostId = journalpostId.getVerdi();
        this.periode = Range.closed(fom, tom);
        this.bostederUtland = bosteder != null ? new LinkedHashSet<>(bosteder) : new LinkedHashSet<>();
    }

    OppgittForutgåendeMedlemskapPeriode(OppgittForutgåendeMedlemskapPeriode other) {
        this.journalpostId = other.journalpostId;
        this.periode = other.periode;
        this.bostederUtland = other.bostederUtland.stream()
            .map(OppgittBosted::new)
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

    public Set<OppgittBosted> getBostederUtland() {
        return Collections.unmodifiableSet(bostederUtland);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittForutgåendeMedlemskapPeriode that = (OppgittForutgåendeMedlemskapPeriode) o;
        return Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(periode, that.periode)
            && Objects.equals(bostederUtland, that.bostederUtland);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, periode, bostederUtland);
    }
}
