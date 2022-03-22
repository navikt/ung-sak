package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.LandkoderKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.UtenlandsoppholdÅrsakConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "UtenlandsoppholdPeriode")
@Table(name = "UP_UTENLANDSOPPHOLD_PERIODE")
@Immutable
public class UtenlandsoppholdPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_UTENLANDSOPPHOLD_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv;

    @Convert(converter = LandkoderKodeverdiConverter.class)
    @Column(name="land", nullable = false)
    private Landkoder land = Landkoder.UDEFINERT;

    @Convert(converter = UtenlandsoppholdÅrsakConverter.class)
    @Column(name = "aarsak")
    private UtenlandsoppholdÅrsak årsak;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UtenlandsoppholdPeriode() {
    }

    public UtenlandsoppholdPeriode(DatoIntervallEntitet periode, boolean aktiv, Landkoder land, UtenlandsoppholdÅrsak årsak) {
        this.periode = periode;
        this.aktiv = aktiv;
        this.land = land;
        this.årsak = årsak;
    }

    public UtenlandsoppholdPeriode(LocalDate fom, LocalDate tom, boolean aktiv, Landkoder land, UtenlandsoppholdÅrsak årsak) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), aktiv, land, årsak);
    }

    public UtenlandsoppholdPeriode(UtenlandsoppholdPeriode utenlandsoppholdPeriode) {
        this(utenlandsoppholdPeriode.periode, utenlandsoppholdPeriode.aktiv, utenlandsoppholdPeriode.land, utenlandsoppholdPeriode.årsak);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public UtenlandsoppholdÅrsak getÅrsak() {
        return årsak;
    }

    public Landkoder getLand() {
        return land;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtenlandsoppholdPeriode that = (UtenlandsoppholdPeriode) o;
        return getPeriode().equals(that.getPeriode()) && land.equals(that.land) && Objects.equals(årsak, that.årsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), land, årsak);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"<" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '>';
    }
}
