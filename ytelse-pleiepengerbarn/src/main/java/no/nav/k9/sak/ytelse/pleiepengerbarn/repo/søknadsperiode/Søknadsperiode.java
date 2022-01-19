package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

@Entity(name = "Søknadsperiode")
@Table(name = "SP_SOEKNADSPERIODE")
@Immutable
public class Søknadsperiode extends BaseEntitet implements SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SP_SOEKNADSPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "har_trukket_krav")
    private boolean harTrukketKrav;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperiode() {
    }

    public Søknadsperiode(DatoIntervallEntitet periode, boolean harTrukketKrav) {
        this.periode = periode;
        this.harTrukketKrav = harTrukketKrav;
    }

    public Søknadsperiode(DatoIntervallEntitet periode) {
        this(periode, false);
    }

    public Søknadsperiode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    public Søknadsperiode(Søknadsperiode it) {
        this.periode = it.getPeriode();
        this.harTrukketKrav = it.isHarTrukketKrav();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public boolean isHarTrukketKrav() {
        return harTrukketKrav;
    }

    @Override
    public <V> V getPayload() {
        // skal returnere data til bruk ved komprimering av perioder (dvs. uten periode)
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Søknadsperiode that = (Søknadsperiode) o;
        return Objects.equals(periode, that.periode) && Objects.equals(harTrukketKrav, that.harTrukketKrav);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, harTrukketKrav);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"<" +
            "id=" + id +
            ", periode=" + periode +
            ", harTrukketKrav=" + harTrukketKrav +
            ", versjon=" + versjon +
            '>';
    }
}
