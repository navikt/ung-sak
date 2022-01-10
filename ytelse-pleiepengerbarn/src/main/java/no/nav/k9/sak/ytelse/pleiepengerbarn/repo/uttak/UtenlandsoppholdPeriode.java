package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

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
