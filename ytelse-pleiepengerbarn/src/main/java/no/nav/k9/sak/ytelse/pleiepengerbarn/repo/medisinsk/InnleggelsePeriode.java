package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "InnleggelsePeriode")
@Table(name = "MD_INNLEGGELSE")
public class InnleggelsePeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_INNLEGGELSE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "legeerklaering_id", nullable = false, updatable = false, unique = true)
    private Legeerklæring legeerklæring;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    InnleggelsePeriode() {
    }

    public InnleggelsePeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public InnleggelsePeriode(InnleggelsePeriode innleggelsePeriode) {
        this.periode = innleggelsePeriode.getPeriode();
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }
    
    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setLegeerklæring(Legeerklæring legeerklæring) {
        this.legeerklæring = legeerklæring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InnleggelsePeriode that = (InnleggelsePeriode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return "InnleggelsePeriode{" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '}';
    }

}
