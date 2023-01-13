package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;

@Entity(name = "VurdertOpplæringPeriode")
@Table(name = "olp_vurdert_opplaering_periode")
@Immutable
public class VurdertOpplæringPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "gjennomfoert_opplaering", nullable = false)
    private Boolean gjennomførtOpplæring;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @OneToMany
    @JoinTable(
        name="OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT",
        joinColumns = @JoinColumn( name="VURDERT_OPPLAERING_PERIODE_ID"),
        inverseJoinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    )
    private List<PleietrengendeSykdomDokument> dokumenter = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæringPeriode() {
    }

    public VurdertOpplæringPeriode(DatoIntervallEntitet periode, Boolean gjennomførtOpplæring, String begrunnelse, List<PleietrengendeSykdomDokument> dokumenter) {
        this.periode = periode;
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.begrunnelse = begrunnelse;
        this.dokumenter = new ArrayList<>(dokumenter);
    }

    public VurdertOpplæringPeriode(LocalDate fom, LocalDate tom, Boolean gjennomførtOpplæring, String begrunnelse, List<PleietrengendeSykdomDokument> dokumenter) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.begrunnelse = begrunnelse;
        this.dokumenter = new ArrayList<>(dokumenter);
    }

    public VurdertOpplæringPeriode(VurdertOpplæringPeriode that) {
        this.periode = that.periode;
        this.gjennomførtOpplæring = that.gjennomførtOpplæring;
        this.begrunnelse = that.begrunnelse;
        this.dokumenter = new ArrayList<>(that.dokumenter);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean getGjennomførtOpplæring() {
        return gjennomførtOpplæring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public List<PleietrengendeSykdomDokument> getDokumenter() {
        return new ArrayList<>(dokumenter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertOpplæringPeriode that = (VurdertOpplæringPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(gjennomførtOpplæring, that.gjennomførtOpplæring)
            && Objects.equals(dokumenter, that.dokumenter)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, gjennomførtOpplæring, begrunnelse, dokumenter);
    }
}
