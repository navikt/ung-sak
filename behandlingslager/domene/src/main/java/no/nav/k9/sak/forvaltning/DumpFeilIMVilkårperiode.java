package no.nav.k9.sak.forvaltning;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "DumpFeilVilkårsperiode")
@Table(name = "DUMP_FEIL_IM_VILKAR_PERIODE")
public class DumpFeilIMVilkårperiode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_FEIL_IM_VILKAR_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    public DumpFeilIMVilkårperiode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public DumpFeilIMVilkårperiode() {
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
