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

@Entity(name = "BeredskapPeriode")
@Table(name = "UP_BEREDSKAP_PERIODE")
@Immutable
public class BeredskapPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_BEREDSKAP_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;
    
    @Column(name = "vil_ha_beredskap")
    private boolean vilHaBeredskap;
    
    @Column(name = "beskrivelse")
    private String beskrivelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    BeredskapPeriode() {
    }

    public BeredskapPeriode(DatoIntervallEntitet periode, boolean vilHaBeredskap, String beskrivelse) {
        this.periode = periode;
        this.vilHaBeredskap = vilHaBeredskap;
        this.beskrivelse = beskrivelse;
    }

    public BeredskapPeriode(LocalDate fom, LocalDate tom, boolean vilHaBeredskap, String beskrivelse) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), vilHaBeredskap, beskrivelse);
    }

    public BeredskapPeriode(BeredskapPeriode feriePeriode) {
        this.periode = feriePeriode.getPeriode();
        this.vilHaBeredskap = feriePeriode.isVilhaBeredskap();
        this.beskrivelse = feriePeriode.getBeskrivelse();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
    
    public boolean isVilhaBeredskap() {
        return vilHaBeredskap;
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
        result = prime * result + (vilHaBeredskap ? 1231 : 1237);
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
        BeredskapPeriode other = (BeredskapPeriode) obj;
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
        if (vilHaBeredskap != other.vilHaBeredskap)
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
