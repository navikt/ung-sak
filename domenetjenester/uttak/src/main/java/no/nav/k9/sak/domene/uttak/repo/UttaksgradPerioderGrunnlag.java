package no.nav.k9.sak.domene.uttak.repo;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "UttaksgradPerioderGrunnlag")
@Table(name = "GR_UTTAKSGRAD")
@DynamicInsert
@DynamicUpdate
public class UttaksgradPerioderGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UTTAKSGRAD")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;


    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "uttaksgrad_perioder_id", nullable = false, updatable = false)
    private UttaksgradPerioder uttaksgradPerioder;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "overstyring_uttaksgrad_perioder_id", nullable = false, updatable = false)
    private OverstyringUttaksgradPerioder overstyringUttaksgradPerioder;


    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttaksgradPerioderGrunnlag() {
    }

    UttaksgradPerioderGrunnlag(UttaksgradPerioderGrunnlag eksisterende) {
    }


}
