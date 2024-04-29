package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "OverstyrtUttakPeriode")
@Table(name = "OVERSTYRT_UTTAK_PERIODE")
@DynamicInsert
@DynamicUpdate
public class OverstyrtUttakPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OVERSTYRT_UTTAK_PERIODE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Embedded
    private DatoIntervallEntitet periode;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "soeker_uttaksgrad")
    private BigDecimal søkersUttaksgrad;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "overstyrt_uttak_periode_id", nullable = false, updatable = false)
    private List<OverstyrtUttakUtbetalingsgradEntitet> overstyrtUtbetalingsgrad;

    @Column(name = "begrunnelse")
    private String begrunnelse;
    @Column(name = "ansvarlig_saksbehandler")
    private String saksbehandler;


    OverstyrtUttakPeriodeEntitet() {
    }

    public OverstyrtUttakPeriodeEntitet(Long behandlingId, DatoIntervallEntitet periode, BigDecimal søkersUttaksgrad, List<OverstyrtUttakUtbetalingsgradEntitet> overstyrtUtbetalingsgrad, String begrunnelse, String saksbehandler) {
        this.behandlingId = behandlingId;
        this.periode = periode;
        this.søkersUttaksgrad = søkersUttaksgrad;
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
        this.begrunnelse = begrunnelse;
        this.saksbehandler = saksbehandler;
    }

    public OverstyrtUttakPeriodeEntitet kopiMedNyPeriode(DatoIntervallEntitet nyPeriode) {
        return new OverstyrtUttakPeriodeEntitet(behandlingId, nyPeriode, søkersUttaksgrad, overstyrtUtbetalingsgrad.stream().map(OverstyrtUttakUtbetalingsgradEntitet::new).toList(), begrunnelse, saksbehandler);
    }

    public BigDecimal getSøkersUttaksgrad() {
        return søkersUttaksgrad;
    }

    public List<OverstyrtUttakUtbetalingsgradEntitet> getOverstyrtUtbetalingsgrad() {
        return overstyrtUtbetalingsgrad;
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    public Long getId() {
        return id;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getSaksbehandler() {
        return saksbehandler;
    }


    @Override
    public String toString() {
        return "OverstyrtUttakPeriodeEntitet{" +
            "behandlingId=" + behandlingId +
            ", periode=" + periode +
            ", søkersUttaksgrad=" + søkersUttaksgrad +
            ", overstyrtUtbetalingsgrad=" + overstyrtUtbetalingsgrad +
            '}';
    }
}
