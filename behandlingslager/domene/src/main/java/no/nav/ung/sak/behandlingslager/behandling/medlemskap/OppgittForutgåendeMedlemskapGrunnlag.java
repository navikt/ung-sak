package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "OppgittForutgåendeMedlemskapGrunnlag")
@Table(name = "GR_OPPGITT_FMEDLEMSKAP")
public class OppgittForutgåendeMedlemskapGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OPPGITT_FMEDLEMSKAP")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @BatchSize(size = 20)
    @JoinColumn(name = "gr_oppgitt_fmedlemskap_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OppgittBosted> bostederUtland = new LinkedHashSet<>();

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public OppgittForutgåendeMedlemskapGrunnlag() {
    }

    public OppgittForutgåendeMedlemskapGrunnlag(Long behandlingId, LocalDate fom, LocalDate tom, Set<OppgittBosted> bosteder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        this.behandlingId = behandlingId;
        this.periode = Range.closed(fom, tom);
        this.bostederUtland = bosteder != null
            ? bosteder.stream().map(OppgittBosted::new).collect(Collectors.toCollection(LinkedHashSet::new))
            : new LinkedHashSet<>();
    }

    OppgittForutgåendeMedlemskapGrunnlag(Long nyBehandlingId, OppgittForutgåendeMedlemskapGrunnlag eksisterende) {
        this.behandlingId = nyBehandlingId;
        this.periode = eksisterende.periode;
        this.bostederUtland = eksisterende.bostederUtland.stream()
            .map(OppgittBosted::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public Set<OppgittBosted> getBostederUtland() {
        return Collections.unmodifiableSet(bostederUtland);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittForutgåendeMedlemskapGrunnlag that = (OppgittForutgåendeMedlemskapGrunnlag) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(bostederUtland, that.bostederUtland);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, bostederUtland);
    }

    @Override
    public String toString() {
        return "OppgittForutgåendeMedlemskapGrunnlag{" +
            "behandlingId=" + behandlingId +
            ", periode=" + periode +
            ", bostederUtland=" + bostederUtland +
            ", aktiv=" + aktiv +
            '}';
    }
}
