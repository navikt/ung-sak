package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

@Entity(name = "UngdomsytelseSøknadsperiodeGrunnlag")
@Table(name = "UNG_GR_SOEKNADSPERIODE")
public class UngdomsytelseSøknadsperiodeGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_GR_SOEKNADSPERIODE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    /**
     * Søknadsperioder som er relevant og som har krav som skal vurderes i behandlingen
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevant_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseSøknadsperioder relevanteSøknadsperioder;

    /**
     * Alle søknadsperioder med krav som har kommet inn i denne og tidligere behandlinger
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitt_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseSøknadsperioder oppgitteSøknadsperioder;

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

    public UngdomsytelseSøknadsperioder getOppgitteSøknadsperioder() {
        return oppgitteSøknadsperioder;
    }

    public UngdomsytelseSøknadsperioder getRelevantSøknadsperioder() {
        return relevanteSøknadsperioder;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(Collection<UngdomsytelseSøknadsperiode> søknadsperioder) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteSøknadsperioder != null ? new HashSet<>(this.oppgitteSøknadsperioder.getPerioder()) : new HashSet<>(søknadsperioder);
        perioder.addAll(søknadsperioder);
        this.oppgitteSøknadsperioder = new UngdomsytelseSøknadsperioder(perioder);
    }

    void setRelevanteSøknadsperioder(UngdomsytelseSøknadsperioder relevanteSøknadsperioder) {
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
