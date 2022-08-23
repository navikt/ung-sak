package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

import jakarta.persistence.*;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;

import java.time.LocalDate;

@Entity(name = "MedisinskGrunnlagsdataSøktPeriode")
@Table(name = "MEDISINSK_GRUNNLAGSDATA_SOEKT_PERIODE")
public class MedisinskGrunnlagsdataSøktPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_SOEKT_PERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_GRUNNLAG_ID", nullable = false)
    private MedisinskGrunnlagsdata medisinskGrunnlagsdata;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

    public MedisinskGrunnlagsdataSøktPeriode() {
        // Hibernate
    }

    public MedisinskGrunnlagsdataSøktPeriode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public MedisinskGrunnlagsdata getSykdomGrunnlag() {
        return medisinskGrunnlagsdata;
    }

    public void setSykdomGrunnlag(MedisinskGrunnlagsdata medisinskGrunnlagsdata) {

        this.medisinskGrunnlagsdata = medisinskGrunnlagsdata;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
