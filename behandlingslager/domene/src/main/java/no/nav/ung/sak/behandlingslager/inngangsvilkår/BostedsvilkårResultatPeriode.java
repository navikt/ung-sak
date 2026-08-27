package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Immutable
@Entity(name = "BostedsvilkårResultatPeriode")
@Table(name = "bosted_resultat_periode")
public class BostedsvilkårResultatPeriode extends BaseEntitet implements VilkårsvurderingResultatPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSTED_RESULTAT_PERIODE")
    @SequenceGenerator(name = "SEQ_BOSTED_RESULTAT_PERIODE", sequenceName = "seq_bosted_resultat_periode", allocationSize = 50)
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "godkjent", nullable = false, updatable = false)
    private boolean godkjent;

    @Enumerated(EnumType.STRING)
    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak;

    @Column(name = "manuell_vurdering", nullable = false, updatable = false)
    private boolean erManuellVurdering;

    @Column(name = "begrunnelse", updatable = false)
    private String begrunnelse;

    @Column(name = "fritekst_vurdering_brev", updatable = false)
    private String fritekstVurderingBrev;

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt",  updatable = false)
    private LocalDateTime vurdertTidspunkt;

    protected BostedsvilkårResultatPeriode() {
        // Hibernate
    }

    public BostedsvilkårResultatPeriode(DatoIntervallEntitet periode, BostedsvilkårResultatPeriode kilde) {
        this(periode, kilde.godkjent, kilde.ikkeOppfyltÅrsak, kilde.erManuellVurdering, kilde.begrunnelse, kilde.fritekstVurderingBrev, kilde.vurdertAv, kilde.vurdertTidspunkt);
    }

    public BostedsvilkårResultatPeriode(DatoIntervallEntitet periode, boolean godkjent, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, boolean erManuellVurdering, String begrunnelse, String fritekstVurderingBrev, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        Objects.requireNonNull(periode, "periode");
        if (erManuellVurdering) {
            Objects.requireNonNull(vurdertAv, "vurdertAv");
            Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");
        }

        if (!godkjent) {
            Objects.requireNonNull(ikkeOppfyltÅrsak, "ikkeOppfyltÅrsak må settes når godkjent=false");
        }
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.godkjent = godkjent;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.erManuellVurdering = erManuellVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstVurderingBrev = fritekstVurderingBrev;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public Long getId() {
        return id;
    }

    @Override
    public VilkårType getVilkårType() {
        return VilkårType.BOSTEDSVILKÅR;
    }

    @Override
    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    @Override
    public boolean isGodkjent() {
        return godkjent;
    }

    @Override
    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
    }

    public boolean erManuellVurdering() {
        return erManuellVurdering;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public String getFritekstVurderingBrev() {
        return fritekstVurderingBrev;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BostedsvilkårResultatPeriode that)) {
            return false;
        }
        return godkjent == that.godkjent
            && erManuellVurdering == that.erManuellVurdering
            && Objects.equals(getPeriode(), that.getPeriode())
            && ikkeOppfyltÅrsak == that.ikkeOppfyltÅrsak
            && Objects.equals(begrunnelse, that.begrunnelse)
            && Objects.equals(fritekstVurderingBrev, that.fritekstVurderingBrev)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), godkjent, ikkeOppfyltÅrsak, erManuellVurdering, begrunnelse, fritekstVurderingBrev, vurdertAv, vurdertTidspunkt);
    }

    @Override
    public String toString() {
        return "BostedsvilkårResultatPeriode{" +
            "id=" + id +
            ", periode=" + periode +
            ", godkjent=" + godkjent +
            ", erManuellVurdering=" + erManuellVurdering +
            ", vurdertTidspunkt=" + vurdertTidspunkt +
            '}';
    }
}

