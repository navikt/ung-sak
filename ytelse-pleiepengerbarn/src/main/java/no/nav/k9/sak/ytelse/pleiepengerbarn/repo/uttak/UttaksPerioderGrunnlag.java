package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.HashSet;
import java.util.Objects;

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

@Entity(name = "UttakPerioderGrunnlag")
@Table(name = "GR_UTTAKSPERIODER")
public class UttaksPerioderGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UTTAKSPERIODER")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevant_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UttakPerioderHolder relevanteSøknadsperioder;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitt_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private UttakPerioderHolder oppgitteSøknadsperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttaksPerioderGrunnlag() {
    }

    UttaksPerioderGrunnlag(Long behandlingId, UttaksPerioderGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.oppgitteSøknadsperioder = grunnlag.oppgitteSøknadsperioder;
        this.relevanteSøknadsperioder = grunnlag.relevanteSøknadsperioder;
    }

    public UttaksPerioderGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getId() {
        return id;
    }

    public UttakPerioderHolder getOppgitteSøknadsperioder() {
        return oppgitteSøknadsperioder;
    }

    public UttakPerioderHolder getRelevantSøknadsperioder() {
        return relevanteSøknadsperioder;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(PerioderFraSøknad perioderFraSøknad) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteSøknadsperioder != null ? new HashSet<>(this.oppgitteSøknadsperioder.getPerioderFraSøknadene()) : new HashSet<PerioderFraSøknad>();
        perioder.add(perioderFraSøknad);
        this.oppgitteSøknadsperioder = new UttakPerioderHolder(perioder);
    }

    void setRelevanteSøknadsperioder(UttakPerioderHolder relevanteSøknadsperioder) {
        Objects.requireNonNull(relevanteSøknadsperioder);
        this.relevanteSøknadsperioder = relevanteSøknadsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UttaksPerioderGrunnlag that = (UttaksPerioderGrunnlag) o;
        return Objects.equals(relevanteSøknadsperioder, that.relevanteSøknadsperioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relevanteSøknadsperioder);
    }
}
