package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
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

    @Column(name = "landkode", nullable = false)
    private String landkode;

    @Column(name = "aarsak")
    private String årsak;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UtenlandsoppholdPeriode() {
    }

    public UtenlandsoppholdPeriode(DatoIntervallEntitet periode, boolean aktiv, String landkode, String årsak) {
        this.periode = periode;
        this.landkode = landkode;
        this.årsak = årsak;
    }

    public UtenlandsoppholdPeriode(LocalDate fom, LocalDate tom, boolean aktiv, String landkode, String årsak) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), aktiv, landkode, årsak);
    }

    public UtenlandsoppholdPeriode(UtenlandsoppholdPeriode utenlandsoppholdPeriode) {
        this(utenlandsoppholdPeriode.periode, utenlandsoppholdPeriode.aktiv, utenlandsoppholdPeriode.landkode, utenlandsoppholdPeriode.årsak);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getLandkode() {
        return landkode;
    }

    public String getÅrsak() {
        return årsak;
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
        return getPeriode().equals(that.getPeriode()) && landkode.equals(that.landkode) && Objects.equals(årsak, that.årsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), landkode, årsak);
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
