package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
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

@Entity(name = "BostedsPeriodeAvklaringForeslått")
@Table(name = "BOSATT_PERIODE_AVKLARING_FORESLAATT")
@Immutable
public class BostedsPeriodeAvklaringForeslått extends BaseEntitet implements BostedsPeriodeAvklaring {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING_FORESLAATT")
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

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", updatable = false)
    private LocalDateTime vurdertTidspunkt;

    public BostedsPeriodeAvklaringForeslått() {
        // Hibernate
    }

    public BostedsPeriodeAvklaringForeslått(DatoIntervallEntitet periode,
                                            BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
                                            String begrunnelse,
                                            boolean skalSendeVarsel,
                                            String fritekstTilVarsel,
                                            String begrunnelseIkkeVarsel,
                                            String vurdertAv,
                                            LocalDateTime vurdertTidspunkt,
                                            Avklaringtype avklaringtype) {
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
    }

    public BostedsPeriodeAvklaringForeslått(BostedsPeriodeAvklaring annenAvklaring) {
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
    }

    public Long getId() {
        return id;
    }

    @Override
    public UUID getReferanse() {
        return referanse;
    }

    @Override
    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
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
        if (!(o instanceof BostedsPeriodeAvklaringForeslått that)) return false;
        return getPeriode().equals(that.getPeriode())
            && ikkeOppfyltÅrsak == that.ikkeOppfyltÅrsak
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
        return Objects.hash(getPeriode(), ikkeOppfyltÅrsak, begrunnelse, skalSendeVarsel, fritekstTilVarsel, begrunnelseIkkeVarsel, vurdertAv, vurdertTidspunkt, avklaringtype);
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
