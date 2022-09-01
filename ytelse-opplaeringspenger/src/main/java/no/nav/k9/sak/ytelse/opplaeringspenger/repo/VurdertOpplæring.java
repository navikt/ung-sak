package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "VurdertOpplæring")
@Table(name = "olp_vurdert_opplaering")
public class VurdertOpplæring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "olp_vurdert_opplaering_grunnlag_id", nullable = false, updatable = false, unique = true)
    private VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "noedvendig_opplaering", nullable = false)
    private Boolean nødvendigOpplæring = false;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertOpplæring() {
    }

    public VurdertOpplæring(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDate fom, LocalDate tom, Boolean nødvendigOpplæring) {
        this.vurdertOpplæringGrunnlag = vurdertOpplæringGrunnlag;
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.nødvendigOpplæring = nødvendigOpplæring;
    }

    public VurdertOpplæring(VurdertOpplæring that) {
        this.vurdertOpplæringGrunnlag = that.vurdertOpplæringGrunnlag;
        this.nødvendigOpplæring = that.nødvendigOpplæring;
        this.periode = that.periode;
    }

    public VurdertOpplæring medGrunnlag(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        this.vurdertOpplæringGrunnlag = vurdertOpplæringGrunnlag;
        return this;
    }

    public VurdertOpplæring medPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return this;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
