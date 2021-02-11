package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.HashSet;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "SøknadsperiodeGrunnlag")
@Table(name = "GR_SOEKNADSPERIODE")
@Immutable
public class SøknadsperiodeGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNADSPERIODE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitt_soknadsperiode_id", nullable = false, updatable = false, unique = true)
    private SøknadsperioderHolder søknadsperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadsperiodeGrunnlag() {
    }

    SøknadsperiodeGrunnlag(Long behandlingId, SøknadsperiodeGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.søknadsperioder = grunnlag.søknadsperioder;
    }

    public Long getId() {
        return id;
    }

    public SøknadsperioderHolder getSøknadsperioder() {
        return søknadsperioder;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(Søknadsperioder søknadsperioder) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = new HashSet<>(this.søknadsperioder.getPerioder());
        perioder.add(søknadsperioder);
        this.søknadsperioder = new SøknadsperioderHolder(perioder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøknadsperiodeGrunnlag that = (SøknadsperiodeGrunnlag) o;
        return Objects.equals(søknadsperioder, that.søknadsperioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknadsperioder);
    }
}
