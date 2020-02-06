package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

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
    private UUID uuid;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @OneToMany(mappedBy = "legeerklæring", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
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

    public Legeerklæring(DatoIntervallEntitet periode, Set<InnleggelsePeriode> perioder, LegeerklæringKilde kilde, String diagnose) {
        Objects.requireNonNull(periode);
        Objects.requireNonNull(perioder);
        this.periode = periode;
        this.kilde = kilde;
        this.diagnose = diagnose;
        this.innleggelsesPerioder = perioder.stream()
            .peek(it -> it.setLegeerklæring(this))
            .collect(Collectors.toSet());
    }

    Legeerklæring(Legeerklæring legeerklæring) {
        this.uuid = legeerklæring.uuid;
        this.periode = legeerklæring.periode;
        this.diagnose = legeerklæring.diagnose;
        this.kilde = legeerklæring.kilde;
        this.innleggelsesPerioder = legeerklæring.getInnleggelsesPerioder()
            .stream()
            .map(InnleggelsePeriode::new)
            .peek(it -> it.setLegeerklæring(this))
            .collect(Collectors.toSet());
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

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public String toString() {
        return "Legeerklæring{" +
            "id=" + id +
            ", uuid=" + uuid +
            ", innleggelsesPerioder=" + innleggelsesPerioder +
            ", legeerklæringer=" + legeerklæringer +
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
