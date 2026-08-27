package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.AvklaringStatus;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregat for bostedsavklaring.
 */
@Entity(name = "BostedsPeriodeAvklaring")
@Table(name = "BOSATT_PERIODE_AVKLARING")
@Immutable
public class BostedsPeriodeAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak;

    @Column(name = "begrunnelse", updatable = false)
    private String begrunnelse;

    @Column(name = "skal_sende_varsel", updatable = false)
    private boolean skalSendeVarsel;

    @Column(name = "fritekst_til_varsel", updatable = false)
    private String fritekstTilVarsel;

    @Column(name = "begrunnelse_ikke_varsel", updatable = false)
    private String begrunnelseIkkeVarsel;

    @Enumerated(EnumType.STRING)
    @Column(name = "avklaringtype", updatable = false, nullable = false)
    private Avklaringtype avklaringtype;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", updatable = false)
    private AvklaringStatus status;

    @Column(name = "opprettet_i_behandling_id", updatable = false, nullable = false)
    private Long opprettetIBehandlingId;

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", updatable = false)
    private LocalDateTime vurdertTidspunkt;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(DatoIntervallEntitet periode, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse, boolean skalSendeVarsel, String fritekstTilVarsel, String begrunnelseIkkeVarsel, String vurdertAv, LocalDateTime vurdertTidspunkt, Avklaringtype avklaringtype, long opprettetIBehandlingId) {
        if (!skalSendeVarsel) {
            Objects.requireNonNull(begrunnelseIkkeVarsel, "Mangler begrunnelse for hvorfor det ikke varsles");
        } else if (BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(ikkeOppfyltÅrsak)) {
            Objects.requireNonNull(fritekstTilVarsel, "Mangler fritekst for varsel når BostedsvilkårIkkeOppfyltÅrsak.ANNET er valgt");
        }

        Objects.requireNonNull(ikkeOppfyltÅrsak, "Mangler årsak for hvorfor bostedsvilkåret ikke er oppfylt");

        Objects.requireNonNull(begrunnelse, "begrunnelse");
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");

        this.periode = periode.toRange();
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.begrunnelse = begrunnelse;
        this.skalSendeVarsel = skalSendeVarsel;
        this.fritekstTilVarsel = fritekstTilVarsel;
        this.begrunnelseIkkeVarsel = begrunnelseIkkeVarsel;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
        this.avklaringtype = avklaringtype;
        this.status = AvklaringStatus.UNDER_ARBEID;
        this.opprettetIBehandlingId = opprettetIBehandlingId;
    }

    public BostedsPeriodeAvklaring(BostedsPeriodeAvklaring annenAvklaring) {
        this.periode = annenAvklaring.getPeriode().toRange();
        this.referanse = annenAvklaring.getReferanse();
        this.ikkeOppfyltÅrsak = annenAvklaring.getIkkeOppfyltÅrsak();
        this.begrunnelse = annenAvklaring.getBegrunnelse();
        this.skalSendeVarsel = annenAvklaring.skalSendeVarsel();
        this.fritekstTilVarsel = annenAvklaring.getFritekstTilVarsel();
        this.begrunnelseIkkeVarsel = annenAvklaring.getBegrunnelseIkkeVarsel();
        this.vurdertAv = annenAvklaring.getVurdertAv();
        this.vurdertTidspunkt = annenAvklaring.getVurdertTidspunkt();
        this.avklaringtype = annenAvklaring.getAvklaringtype();
        this.status = annenAvklaring.getStatus();
        this.opprettetIBehandlingId = annenAvklaring.getOpprettetIBehandlingId();
    }

    private BostedsPeriodeAvklaring(DatoIntervallEntitet periode, UUID referanse, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse, boolean skalSendeVarsel, String fritekstTilVarsel, String begrunnelseIkkeVarsel, String vurdertAv, LocalDateTime vurdertTidspunkt, Avklaringtype avklaringtype, AvklaringStatus status, long opprettetIBehandlingId) {
        this.periode = periode.toRange();
        this.referanse = referanse;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.begrunnelse = begrunnelse;
        this.skalSendeVarsel = skalSendeVarsel;
        this.fritekstTilVarsel = fritekstTilVarsel;
        this.begrunnelseIkkeVarsel = begrunnelseIkkeVarsel;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
        this.avklaringtype = avklaringtype;
        this.status = status;
        this.opprettetIBehandlingId = opprettetIBehandlingId;
    }

    public BostedsPeriodeAvklaring medNyPeriode(DatoIntervallEntitet nyPeriode) {
        return new BostedsPeriodeAvklaring(
            nyPeriode,
            this.referanse,
            this.ikkeOppfyltÅrsak,
            this.begrunnelse,
            this.skalSendeVarsel,
            this.fritekstTilVarsel,
            this.begrunnelseIkkeVarsel,
            this.vurdertAv,
            this.vurdertTidspunkt,
            this.avklaringtype,
            this.status,
            this.opprettetIBehandlingId
        );
    }

    public BostedsPeriodeAvklaring medStatusFerdig() {
        return new BostedsPeriodeAvklaring(
            getPeriode(),
            this.referanse,
            this.ikkeOppfyltÅrsak,
            this.begrunnelse,
            this.skalSendeVarsel,
            this.fritekstTilVarsel,
            this.begrunnelseIkkeVarsel,
            this.vurdertAv,
            this.vurdertTidspunkt,
            this.avklaringtype,
            AvklaringStatus.FERDIG,
            this.opprettetIBehandlingId
        );
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilVarsel() {
        return fritekstTilVarsel;
    }

    public boolean skalSendeVarsel() {
        return skalSendeVarsel;
    }

    public String getBegrunnelseIkkeVarsel() {
        return begrunnelseIkkeVarsel;
    }

    public Avklaringtype getAvklaringtype() {
        return avklaringtype;
    }

    public Long getOpprettetIBehandlingId() {
        return opprettetIBehandlingId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return getPeriode().equals(that.getPeriode())
            && ikkeOppfyltÅrsak == that.ikkeOppfyltÅrsak
            && Objects.equals(begrunnelse, that.begrunnelse)
            && skalSendeVarsel == that.skalSendeVarsel
            && Objects.equals(fritekstTilVarsel, that.fritekstTilVarsel)
            && Objects.equals(begrunnelseIkkeVarsel, that.begrunnelseIkkeVarsel)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt)
            && avklaringtype == that.avklaringtype
            && status == that.status
            && Objects.equals(opprettetIBehandlingId, that.opprettetIBehandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), ikkeOppfyltÅrsak, begrunnelse, skalSendeVarsel, fritekstTilVarsel, begrunnelseIkkeVarsel, vurdertAv, vurdertTidspunkt, avklaringtype, status, opprettetIBehandlingId);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{referanse=" + referanse
            + ", periode=" + periode
            + ", skalSendeVarsel=" + skalSendeVarsel
            + ", avklaringtype=" + avklaringtype
            + ", vurdertTidspunkt=" + vurdertTidspunkt
            + ", status=" + status
            + ", opprettetIBehandlingId=" + opprettetIBehandlingId + '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public AvklaringStatus getStatus() {
        return status;
    }

    public boolean kanRedigeres() {
        return status == AvklaringStatus.UNDER_ARBEID;
    }
}
