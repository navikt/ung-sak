package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;

@Entity(name = "UngdomsytelseSøknadsperiodeGrunnlag")
@Table(name = "UNG_GR_SOEKNADSPERIODE")
public class UngdomsytelseSøknadsperiodeGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNADSPERIODE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevant_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseSøknadsperioderHolder relevanteSøknadsperioder;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitt_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseSøknadsperioderHolder oppgitteSøknadsperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperiodeGrunnlag() {
    }

    UngdomsytelseSøknadsperiodeGrunnlag(Long behandlingId, UngdomsytelseSøknadsperiodeGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.oppgitteSøknadsperioder = grunnlag.oppgitteSøknadsperioder;
        this.relevanteSøknadsperioder = grunnlag.relevanteSøknadsperioder;
    }

    public UngdomsytelseSøknadsperiodeGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getId() {
        return id;
    }

    public UngdomsytelseSøknadsperioderHolder getOppgitteSøknadsperioder() {
        return oppgitteSøknadsperioder;
    }

    public UngdomsytelseSøknadsperioderHolder getRelevantSøknadsperioder() {
        return relevanteSøknadsperioder;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(UngdomsytelseSøknadsperioder søknadsperioder) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteSøknadsperioder != null ? new HashSet<>(this.oppgitteSøknadsperioder.getPerioder()) : new HashSet<>(Set.of(søknadsperioder));
        perioder.add(søknadsperioder);
        this.oppgitteSøknadsperioder = new UngdomsytelseSøknadsperioderHolder(perioder);
    }

    void setRelevanteSøknadsperioder(UngdomsytelseSøknadsperioderHolder relevanteSøknadsperioder) {
        Objects.requireNonNull(relevanteSøknadsperioder);
        this.relevanteSøknadsperioder = relevanteSøknadsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperiodeGrunnlag that = (UngdomsytelseSøknadsperiodeGrunnlag) o;
        return Objects.equals(oppgitteSøknadsperioder, that.oppgitteSøknadsperioder)
            && Objects.equals(relevanteSøknadsperioder, that.relevanteSøknadsperioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgitteSøknadsperioder, relevanteSøknadsperioder);
    }
}
