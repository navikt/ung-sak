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

@Entity(name = "NattevåkPeriode")
@Table(name = "UP_NATTEVAAK_PERIODE")
@Immutable
public class NattevåkPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_NATTEVAAK_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;
    
    @Column(name = "vil_ha_nattevaak")
    private boolean vilHaNattevåk;
    
    @Column(name = "beskrivelse")
    private String beskrivelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NattevåkPeriode() {
    }

    public NattevåkPeriode(DatoIntervallEntitet periode, boolean vilHaNattevåk, String beskrivelse) {
        this.periode = periode;
        this.vilHaNattevåk = vilHaNattevåk;
        this.beskrivelse = beskrivelse;
    }

    public NattevåkPeriode(LocalDate fom, LocalDate tom, boolean vilHaNattevåk, String beskrivelse) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), vilHaNattevåk, beskrivelse);
    }

    public NattevåkPeriode(NattevåkPeriode feriePeriode) {
        this.periode = feriePeriode.getPeriode();
        this.vilHaNattevåk = feriePeriode.isVilhaNattevåk();
        this.beskrivelse = feriePeriode.getBeskrivelse();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
    
    public boolean isVilhaNattevåk() {
        return vilHaNattevåk;
    }
    
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beskrivelse == null) ? 0 : beskrivelse.hashCode());
        result = prime * result + ((periode == null) ? 0 : periode.hashCode());
        result = prime * result + (vilHaNattevåk ? 1231 : 1237);
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
        NattevåkPeriode other = (NattevåkPeriode) obj;
        if (beskrivelse == null) {
            if (other.beskrivelse != null)
                return false;
        } else if (!beskrivelse.equals(other.beskrivelse))
            return false;
        if (periode == null) {
            if (other.periode != null)
                return false;
        } else if (!periode.equals(other.periode))
            return false;
        if (vilHaNattevåk != other.vilHaNattevåk)
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
