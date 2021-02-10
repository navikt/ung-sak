package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "Søknadsperiode")
@Table(name = "GR_SOEKNADSPERIODE")
@Immutable
public class SøknadsperiodeGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_SOEKNADSPERIODE")
    private Long id;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "søknadsperioder_id", nullable = false, updatable = false, unique = true)
    private SøknadsperioderHolder søknadsperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadsperiodeGrunnlag() {
    }

    SøknadsperiodeGrunnlag(SøknadsperiodeGrunnlag grunnlag) {
        søknadsperioder = grunnlag.søknadsperioder;
    }

    public Long getId() {
        return id;
    }

    public SøknadsperioderHolder getSøknadsperioder() {
        return søknadsperioder;
    }

    public boolean isAktiv() {
        return aktiv;
    }
}
