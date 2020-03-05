package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.LegeerklæringKildeKodeverkConverter;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.medisinsk.LegeerklæringKilde;

@Entity(name = "Legeerklæring")
@Table(name = "MD_LEGEERKLAERING")
public class Legeerklæring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_LEGEERKLAERING")
    private Long id;

    @Column(name = "referanse", nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "fom", nullable = false)
    private LocalDate datert;

    @OneToMany(mappedBy = "legeerklæring", cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<InnleggelsePeriode> innleggelsesPerioder;

    @ManyToOne
    @JoinColumn(name = "legeerklaeringer_id", nullable = false, updatable = false, unique = true)
    private Legeerklæringer legeerklæringer;

    @Convert(converter = LegeerklæringKildeKodeverkConverter.class)
    @Column(name = "kilde", nullable = false)
    private LegeerklæringKilde kilde;

    @Column(name = "diagnose", nullable = false)
    private String diagnose;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Legeerklæring() {
        // hibernate
    }

    public Legeerklæring(LocalDate periode, Set<InnleggelsePeriode> perioder, LegeerklæringKilde kilde, String diagnose) {
        Objects.requireNonNull(periode);
        Objects.requireNonNull(perioder);
        this.datert = periode;
        this.kilde = kilde;
        this.diagnose = diagnose;
        this.innleggelsesPerioder = perioder.stream()
            .peek(it -> it.setLegeerklæring(this))
            .collect(Collectors.toSet());
    }

    Legeerklæring(Legeerklæring legeerklæring) {
        this.uuid = legeerklæring.uuid;
        this.datert = legeerklæring.datert;
        this.diagnose = legeerklæring.diagnose;
        this.kilde = legeerklæring.kilde;
        this.innleggelsesPerioder = legeerklæring.getInnleggelsesPerioder()
            .stream()
            .map(InnleggelsePeriode::new)
            .peek(it -> it.setLegeerklæring(this))
            .collect(Collectors.toSet());
    }

    public Legeerklæring(UUID identifikator, LocalDate datert, Set<InnleggelsePeriode> innleggelsePerioder, LegeerklæringKilde kilde, String diagnosekode) {
        this(datert, innleggelsePerioder, kilde, diagnosekode);
        if(identifikator != null) {
            this.uuid = identifikator;
        }
    }

    public Long getId() {
        return id;
    }

    public Set<InnleggelsePeriode> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    void setLegeerklæringer(Legeerklæringer legeerklæringer) {
        this.legeerklæringer = legeerklæringer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public LegeerklæringKilde getKilde() {
        return kilde;
    }

    public String getDiagnose() {
        return diagnose;
    }

    public LocalDate getDatert() {
        return datert;
    }

    @Override
    public String toString() {
        return "Legeerklæring{" +
            "id=" + id +
            ", uuid=" + uuid +
            ", innleggelsesPerioder=" + innleggelsesPerioder +
            ", versjon=" + versjon +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Legeerklæring fordeling = (Legeerklæring) o;
        return Objects.equals(uuid, fordeling.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
