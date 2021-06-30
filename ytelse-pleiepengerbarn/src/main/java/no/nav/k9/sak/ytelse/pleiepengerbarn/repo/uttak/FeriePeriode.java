package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;

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

@Entity(name = "PsbFeriePeriode")
@Table(name = "UP_FERIE_PERIODE")
@Immutable
public class FeriePeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_FERIE_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;
    
    @Column(name = "skal_ha_ferie")
    private boolean skalHaFerie;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FeriePeriode() {
    }

    public FeriePeriode(DatoIntervallEntitet periode, boolean skalHaFerie) {
        this.periode = periode;
        this.skalHaFerie = skalHaFerie;
    }

    public FeriePeriode(LocalDate fom, LocalDate tom, boolean skalHaFerie) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), skalHaFerie);
    }

    public FeriePeriode(FeriePeriode feriePeriode) {
        this.periode = feriePeriode.getPeriode();
        this.skalHaFerie = feriePeriode.isSkalHaFerie();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
    
    public boolean isSkalHaFerie() {
        return skalHaFerie;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((periode == null) ? 0 : periode.hashCode());
        result = prime * result + (skalHaFerie ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeriePeriode other = (FeriePeriode) obj;
        if (periode == null) {
            if (other.periode != null)
                return false;
        } else if (!periode.equals(other.periode))
            return false;
        if (skalHaFerie != other.skalHaFerie)
            return false;
        return true;
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
