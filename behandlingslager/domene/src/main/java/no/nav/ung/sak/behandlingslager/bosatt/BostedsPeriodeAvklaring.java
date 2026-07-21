package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
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

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

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

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", updatable = false)
    private LocalDateTime vurdertTidspunkt;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(DatoIntervallEntitet periode, boolean erBosattITrondheim, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse, boolean skalSendeVarsel, String fritekstTilVarsel, String begrunnelseIkkeVarsel, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        if (!skalSendeVarsel) {
            Objects.requireNonNull(begrunnelseIkkeVarsel, "Mangler begrunnelse for hvorfor det ikke varsles");
        } else if (BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(ikkeOppfyltÅrsak)) {
            Objects.requireNonNull(fritekstTilVarsel, "Mangler fritekst for varsel når BostedsvilkårIkkeOppfyltÅrsak.ANNET er valgt");
        }

        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");

        this.periode = periode.toRange();
        this.erBosattITrondheim = erBosattITrondheim;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.begrunnelse = begrunnelse;
        this.skalSendeVarsel = skalSendeVarsel;
        this.fritekstTilVarsel = fritekstTilVarsel;
        this.begrunnelseIkkeVarsel = begrunnelseIkkeVarsel;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public BostedsPeriodeAvklaring(BostedsPeriodeAvklaring annenAvklaring) {
        this.periode = annenAvklaring.getPeriode().toRange();
        this.referanse = annenAvklaring.getReferanse();
        this.erBosattITrondheim = annenAvklaring.isErBosattITrondheim();
        this.ikkeOppfyltÅrsak = annenAvklaring.getIkkeOppfyltÅrsak();
        this.begrunnelse = annenAvklaring.getBegrunnelse();
        this.skalSendeVarsel = annenAvklaring.skalSendeVarsel();
        this.fritekstTilVarsel = annenAvklaring.getFritekstTilVarsel();
        this.begrunnelseIkkeVarsel = annenAvklaring.getBegrunnelseIkkeVarsel();
        this.vurdertAv = annenAvklaring.getVurdertAv();
        this.vurdertTidspunkt = annenAvklaring.getVurdertTidspunkt();
    }

    private BostedsPeriodeAvklaring(DatoIntervallEntitet periode, UUID referanse, boolean erBosattITrondheim, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse, boolean skalSendeVarsel, String fritekstTilVarsel, String begrunnelseIkkeVarsel, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.periode = periode.toRange();
        this.referanse = referanse;
        this.erBosattITrondheim = erBosattITrondheim;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.begrunnelse = begrunnelse;
        this.skalSendeVarsel = skalSendeVarsel;
        this.fritekstTilVarsel = fritekstTilVarsel;
        this.begrunnelseIkkeVarsel = begrunnelseIkkeVarsel;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public BostedsPeriodeAvklaring medNyPeriode(DatoIntervallEntitet nyPeriode) {
        return new BostedsPeriodeAvklaring(
            nyPeriode,
            this.referanse,
            this.erBosattITrondheim,
            this.ikkeOppfyltÅrsak,
            this.begrunnelse,
            this.skalSendeVarsel,
            this.fritekstTilVarsel,
            this.begrunnelseIkkeVarsel,
            this.vurdertAv,
            this.vurdertTidspunkt
        );
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return periode.equals(that.periode)
            && erBosattITrondheim == that.erBosattITrondheim
            && ikkeOppfyltÅrsak == that.ikkeOppfyltÅrsak
            && Objects.equals(begrunnelse, that.begrunnelse)
            && skalSendeVarsel == that.skalSendeVarsel
            && Objects.equals(fritekstTilVarsel, that.fritekstTilVarsel)
            && Objects.equals(begrunnelseIkkeVarsel, that.begrunnelseIkkeVarsel)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, erBosattITrondheim, ikkeOppfyltÅrsak, begrunnelse, skalSendeVarsel, fritekstTilVarsel, begrunnelseIkkeVarsel, vurdertAv, vurdertTidspunkt);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{referanse=" + referanse
            + ", periode=" + periode
            + ", erBosattITrondheim=" + erBosattITrondheim
            + ", ikkeOppfyltÅrsak=" + ikkeOppfyltÅrsak
            + ", skalSendeVarsel=" + skalSendeVarsel
            + ", vurdertAv=" + vurdertAv
            + ", vurdertTidspunkt=" + vurdertTidspunkt + '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }
}
