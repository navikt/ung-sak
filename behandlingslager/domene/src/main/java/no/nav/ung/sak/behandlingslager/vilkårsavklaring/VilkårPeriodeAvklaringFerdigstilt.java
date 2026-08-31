package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
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
 * Vilkårsavklaring som er ferdigstilt, dvs. vedtatt i en behandling. Ferdigstilte avklaringer akkumuleres
 * og kopieres videre til nye behandlinger.
 */
@Entity(name = "VilkårPeriodeAvklaringFerdigstilt")
@Table(name = "VILKAAR_PERIODE_AVKLARING")
@Immutable
public class VilkårPeriodeAvklaringFerdigstilt extends BaseEntitet implements VilkårPeriodeAvklaring {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAAR_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private String ikkeOppfyltÅrsakKode;

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

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", updatable = false)
    private LocalDateTime vurdertTidspunkt;

    public VilkårPeriodeAvklaringFerdigstilt() {
        // Hibernate
    }

    VilkårPeriodeAvklaringFerdigstilt(VilkårPeriodeAvklaring annenAvklaring) {
        this(annenAvklaring, annenAvklaring.getPeriode());
    }

    VilkårPeriodeAvklaringFerdigstilt(VilkårPeriodeAvklaring annenAvklaring, DatoIntervallEntitet nyPeriode) {
        this.periode = nyPeriode.toRange();
        this.referanse = annenAvklaring.getReferanse();
        this.ikkeOppfyltÅrsakKode = annenAvklaring.getIkkeOppfyltÅrsakKode();
        this.begrunnelse = annenAvklaring.getBegrunnelse();
        this.skalSendeVarsel = annenAvklaring.skalSendeVarsel();
        this.fritekstTilVarsel = annenAvklaring.getFritekstTilVarsel();
        this.begrunnelseIkkeVarsel = annenAvklaring.getBegrunnelseIkkeVarsel();
        this.vurdertAv = annenAvklaring.getVurdertAv();
        this.vurdertTidspunkt = annenAvklaring.getVurdertTidspunkt();
        this.avklaringtype = annenAvklaring.getAvklaringtype();
    }

    public Long getId() {
        return id;
    }

    @Override
    public UUID getReferanse() {
        return referanse;
    }

    @Override
    public String getIkkeOppfyltÅrsakKode() {
        return ikkeOppfyltÅrsakKode;
    }

    @Override
    public String getVurdertAv() {
        return vurdertAv;
    }

    @Override
    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public String getFritekstTilVarsel() {
        return fritekstTilVarsel;
    }

    @Override
    public boolean skalSendeVarsel() {
        return skalSendeVarsel;
    }

    @Override
    public String getBegrunnelseIkkeVarsel() {
        return begrunnelseIkkeVarsel;
    }

    @Override
    public Avklaringtype getAvklaringtype() {
        return avklaringtype;
    }

    @Override
    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    // Referanse er bevisst utelatt fra equals/hashCode, slik at lagring av identisk innhold ikke gir en ny
    // avklaring (og dermed ny referanse) — referansen må være stabil så lenge innholdet er uendret.
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VilkårPeriodeAvklaringFerdigstilt that)) return false;
        return getPeriode().equals(that.getPeriode())
            && Objects.equals(ikkeOppfyltÅrsakKode, that.ikkeOppfyltÅrsakKode)
            && Objects.equals(begrunnelse, that.begrunnelse)
            && skalSendeVarsel == that.skalSendeVarsel
            && Objects.equals(fritekstTilVarsel, that.fritekstTilVarsel)
            && Objects.equals(begrunnelseIkkeVarsel, that.begrunnelseIkkeVarsel)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt)
            && avklaringtype == that.avklaringtype;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), ikkeOppfyltÅrsakKode, begrunnelse, skalSendeVarsel, fritekstTilVarsel, begrunnelseIkkeVarsel, vurdertAv, vurdertTidspunkt, avklaringtype);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{referanse=" + referanse
            + ", periode=" + periode
            + ", skalSendeVarsel=" + skalSendeVarsel
            + ", avklaringtype=" + avklaringtype
            + ", vurdertTidspunkt=" + vurdertTidspunkt + '}';
    }
}
