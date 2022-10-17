package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

@Entity(name = "GodkjentOpplæringsinstitusjon")
@Table(name = "GODKJENTE_OPPLAERINGSINSTITUSJONER")
public class GodkjentOpplæringsinstitusjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GODKJENTE_OPPLAERINGSINSTITUSJONER")
    private Long id;

    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "navn", nullable = false)
    private String navn;

    @BatchSize(size = 20)
    @JoinColumn(name = "institusjon_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private List<GodkjentOpplæringsinstitusjonPeriode> perioder;

    public GodkjentOpplæringsinstitusjon() {
    }

    public GodkjentOpplæringsinstitusjon(UUID uuid, String navn, List<GodkjentOpplæringsinstitusjonPeriode> perioder) {
        this.uuid = uuid;
        this.navn = navn;
        this.perioder = perioder;
    }

    public GodkjentOpplæringsinstitusjon(UUID uuid, String navn, LocalDate fomDato, LocalDate tomDato) {
        this.uuid = uuid;
        this.navn = navn;
        this.perioder = List.of(new GodkjentOpplæringsinstitusjonPeriode(fomDato, tomDato));
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNavn() {
        return navn;
    }

    public List<GodkjentOpplæringsinstitusjonPeriode> getPerioder() {
        return perioder;
    }

    public LocalDateTimeline<Boolean> getTidslinje() {
        return TidslinjeUtil.tilTidslinjeKomprimert(perioder.stream()
            .map(GodkjentOpplæringsinstitusjonPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GodkjentOpplæringsinstitusjon that = (GodkjentOpplæringsinstitusjon) o;
        return Objects.equals(uuid, that.uuid)
            && Objects.equals(navn, that.navn)
            && Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, navn, perioder);
    }

    @Override
    public String toString() {
        return "GodkjentInstitusjon{" +
            "uuid=" + uuid +
            ", navn=" + navn +
            ", perioder=" + perioder +
            '}';
    }
}
