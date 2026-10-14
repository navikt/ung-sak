package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
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
import java.util.Optional;

@Immutable
@Entity(name = "AndreLivsoppholdsytelserResultatPeriode")
@Table(name = "livsopphold_resultat_periode")
public class AndreLivsoppholdsytelserResultatPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LIVSOPPHOLD_RESULTAT_PERIODE")
    @SequenceGenerator(name = "SEQ_LIVSOPPHOLD_RESULTAT_PERIODE", sequenceName = "seq_livsopphold_resultat_periode", allocationSize = 50)
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "godkjent", nullable = false, updatable = false)
    private boolean godkjent;

    @Enumerated(EnumType.STRING)
    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak;

    @Column(name = "manuell_vurdering", nullable = false, updatable = false)
    private boolean manuellVurdering;

    @Column(name = "begrunnelse", updatable = false)
    private String begrunnelse;

    @Column(name = "fritekst_vurdering_brev", updatable = false)
    private String fritekstVurderingBrev;

    @Column(name = "ytelse_navn", updatable = false)
    private String ytelseNavn;

    @Column(name = "vurdert_av", nullable = false, updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", nullable = false, updatable = false)
    private LocalDateTime vurdertTidspunkt;

    @Column(name = "regel_input", updatable = false)
    private String regelInput;

    @Column(name = "regel_evaluering", updatable = false)
    private String regelEvaluering;

    AndreLivsoppholdsytelserResultatPeriode() {
        // Hibernate
    }

    /** Oppretter en kopi med ny periode, men med verdiene fra kildeentiteten. Brukes ved sammenslåing av tidslinjer. */
    public AndreLivsoppholdsytelserResultatPeriode(DatoIntervallEntitet periode, AndreLivsoppholdsytelserResultatPeriode kilde) {
        this(periode, kilde.godkjent, kilde.ikkeOppfyltÅrsak, kilde.manuellVurdering, kilde.begrunnelse, kilde.fritekstVurderingBrev, kilde.ytelseNavn, kilde.vurdertAv, kilde.vurdertTidspunkt, kilde.getRegelsporing().orElse(null));
    }

    AndreLivsoppholdsytelserResultatPeriode(AndreLivsoppholdsytelserResultatPeriode kilde) {
        this(kilde.getPeriode(), kilde);
    }

    public AndreLivsoppholdsytelserResultatPeriode(DatoIntervallEntitet periode, AndreLivsoppholdsytelserVurderingResultat vilkårsvurderingResultat, Regelsporing regelsporing) {
        this(periode,
            vilkårsvurderingResultat.godkjent(),
            (AndreLivsoppholdsytelserIkkeOppfyltÅrsak) vilkårsvurderingResultat.ikkeOppfyltÅrsak(),
            vilkårsvurderingResultat.erManuellVurdering(),
            vilkårsvurderingResultat.begrunnelse(),
            vilkårsvurderingResultat.fritekstVurderingBrev(),
            vilkårsvurderingResultat.ytelseNavn(),
            vilkårsvurderingResultat.vurdertAv(),
            vilkårsvurderingResultat.vurdertTidspunkt(),
            regelsporing);
    }

    public AndreLivsoppholdsytelserResultatPeriode(DatoIntervallEntitet periode, boolean godkjent, AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, boolean manuellVurdering, String begrunnelse, String fritekstVurderingBrev, String ytelseNavn, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this(periode, godkjent, ikkeOppfyltÅrsak, manuellVurdering, begrunnelse, fritekstVurderingBrev, ytelseNavn, vurdertAv, vurdertTidspunkt, null);
    }

    public AndreLivsoppholdsytelserResultatPeriode(DatoIntervallEntitet periode, boolean godkjent, AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, boolean manuellVurdering, String begrunnelse, String fritekstVurderingBrev, String ytelseNavn, String vurdertAv, LocalDateTime vurdertTidspunkt, Regelsporing regelsporing) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(vurdertAv, "vurdertAv");
        Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");
        if (!godkjent) {
            Objects.requireNonNull(ikkeOppfyltÅrsak, "ikkeOppfyltÅrsak må settes når godkjent=false");
        }
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.godkjent = godkjent;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.manuellVurdering = manuellVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstVurderingBrev = fritekstVurderingBrev;
        this.ytelseNavn = ytelseNavn;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
        this.regelInput = regelsporing != null ? regelsporing.input() : null;
        this.regelEvaluering = regelsporing != null ? regelsporing.evaluering() : null;
    }

    public Long getId() {
        return id;
    }

    public VilkårType getVilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public AndreLivsoppholdsytelserIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
    }

    public boolean isManuellVurdering() {
        return manuellVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstVurderingBrev() {
        return fritekstVurderingBrev;
    }

    public String getYtelseNavn() {
        return ytelseNavn;
    }

    public VilkårsvurderingResultat tilVilkårsvurderingResultat() {
        return new AndreLivsoppholdsytelserVurderingResultat(godkjent, ikkeOppfyltÅrsak, manuellVurdering, begrunnelse, fritekstVurderingBrev, ytelseNavn, vurdertAv, vurdertTidspunkt);
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    public Optional<Regelsporing> getRegelsporing() {
        return regelInput == null ? Optional.empty() : Optional.of(new Regelsporing(regelInput, regelEvaluering));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AndreLivsoppholdsytelserResultatPeriode annen
            && DatoIntervallEntitet.fra(periode).equals(DatoIntervallEntitet.fra(annen.periode))
            && godkjent == annen.godkjent
            && ikkeOppfyltÅrsak == annen.ikkeOppfyltÅrsak
            && manuellVurdering == annen.manuellVurdering
            && Objects.equals(begrunnelse, annen.begrunnelse)
            && Objects.equals(fritekstVurderingBrev, annen.fritekstVurderingBrev)
            && Objects.equals(ytelseNavn, annen.ytelseNavn)
            && Objects.equals(vurdertAv, annen.vurdertAv)
            && Objects.equals(vurdertTidspunkt, annen.vurdertTidspunkt)
            && Objects.equals(regelInput, annen.regelInput)
            && Objects.equals(regelEvaluering, annen.regelEvaluering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(DatoIntervallEntitet.fra(periode), vurdertTidspunkt);
    }
}
