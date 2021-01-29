package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "SykdomDiagnosekode")
@Table(name = "SYKDOM_DIAGNOSEKODE")
class SykdomDiagnosekode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DIAGNOSEKODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_DIAGNOSEKODER_ID")
    private SykdomDiagnosekoder diagnosekoder;

    @Column(name = "DIAGNOSEKODE", nullable = false, updatable=false)
    private String diagnosekode;

    SykdomDiagnosekode() {
        //Hibernate
    }

    public SykdomDiagnosekode(SykdomDiagnosekoder diagnosekoder, String diagnosekode) {
        this.diagnosekoder = diagnosekoder;
        this.diagnosekode = diagnosekode;
    }

    public SykdomDiagnosekoder getDiagnosekoder() {
        return diagnosekoder;
    }

    public String getDiagnosekode() {
        return diagnosekode;
    }
}
