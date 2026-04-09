package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
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
    @Column(name = "forutgaaende_periode", columnDefinition = "daterange")
    private Range<LocalDate> forutgåendePeriode;

    @BatchSize(size = 20)
    @JoinColumn(name = "gr_oppgitt_fmedlemskap_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OppgittBosted> bosteder = new LinkedHashSet<>();

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public OppgittForutgåendeMedlemskapGrunnlag() {
    }

    public OppgittForutgåendeMedlemskapGrunnlag(Long behandlingId, LocalDate forutgåendePeriodeFom, LocalDate forutgåendePeriodeTom, Set<OppgittBosted> bosteder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(forutgåendePeriodeFom, "forutgåendePeriodeFom");
        Objects.requireNonNull(forutgåendePeriodeTom, "forutgåendePeriodeTom");
        this.behandlingId = behandlingId;
        this.forutgåendePeriode = Range.closed(forutgåendePeriodeFom, forutgåendePeriodeTom);
        this.bosteder = bosteder != null
            ? bosteder.stream().map(OppgittBosted::new).collect(Collectors.toCollection(LinkedHashSet::new))
            : new LinkedHashSet<>();
    }

    OppgittForutgåendeMedlemskapGrunnlag(Long nyBehandlingId, OppgittForutgåendeMedlemskapGrunnlag eksisterende) {
        this.behandlingId = nyBehandlingId;
        this.forutgåendePeriode = eksisterende.forutgåendePeriode;
        this.bosteder = eksisterende.bosteder.stream()
            .map(OppgittBosted::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Range<LocalDate> getForutgåendePeriode() {
        return forutgåendePeriode;
    }

    public LocalDate getForutgåendePeriodeFom() {
        return forutgåendePeriode.lower();
    }

    public LocalDate getForutgåendePeriodeTom() {
        return forutgåendePeriode.upper();
    }

    public Set<OppgittBosted> getBosteder() {
        return Collections.unmodifiableSet(bosteder);
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
        return Objects.equals(forutgåendePeriode, that.forutgåendePeriode)
            && Objects.equals(bosteder, that.bosteder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forutgåendePeriode, bosteder);
    }

    @Override
    public String toString() {
        return "OppgittForutgåendeMedlemskapGrunnlag{" +
            "behandlingId=" + behandlingId +
            ", forutgåendePeriode=" + forutgåendePeriode +
            ", bosteder=" + bosteder +
            ", aktiv=" + aktiv +
            '}';
    }
}
