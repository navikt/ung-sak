package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import org.hibernate.annotations.Immutable;

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
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "VurdertReisetidPeriode")
@Table(name = "olp_vurdert_reisetid_periode")
@Immutable
public class VurdertReisetidPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_REISETID_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "godkjent", nullable = false)
    private Boolean godkjent;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertReisetidPeriode() {
    }

    public VurdertReisetidPeriode(DatoIntervallEntitet periode, Boolean godkjent) {
        this.periode = periode;
        this.godkjent = godkjent;
    }

    public VurdertReisetidPeriode(VurdertReisetidPeriode that) {
        this.periode = that.periode;
        this.godkjent = that.godkjent;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean getGodkjent() {
        return godkjent;
    }
}
