package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity(name = "AktivitetspengerInngangsvilkårResultatGrunnlag")
@Table(name = "gr_akt_inngangsvilkaar_res")
public class AktivitetspengerInngangsvilkårResultatGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_AKT_INNGANGSVILKAAR_RES")
    @SequenceGenerator(name = "SEQ_GR_AKT_INNGANGSVILKAAR_RES", sequenceName = "seq_gr_akt_inngangsvilkaar_res", allocationSize = 50)
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bistand_resultat_holder_id", updatable = false)
    private BistandsvilkårResultatHolder bistandsvilkårResultatHolder;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "aktivitet_resultat_holder_id", updatable = false)
    private AktivitetsvilkårResultatHolder aktivitetsvilkårResultatHolder;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "livsopphold_resultat_holder_id", updatable = false)
    private AndreLivsoppholdsytelserResultatHolder andreLivsoppholdsytelserResultatHolder;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bosted_resultat_holder_id", updatable = false)
    private BostedsvilkårResultatHolder bostedsvilkårResultatHolder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public AktivitetspengerInngangsvilkårResultatGrunnlag() {
    }

    AktivitetspengerInngangsvilkårResultatGrunnlag(Long behandlingId,
                                                   BistandsvilkårResultatHolder bistandHolder,
                                                   AktivitetsvilkårResultatHolder aktivitetHolder,
                                                   AndreLivsoppholdsytelserResultatHolder livsoppholdHolder,
                                                   BostedsvilkårResultatHolder bostedHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
        this.bistandsvilkårResultatHolder = bistandHolder;
        this.aktivitetsvilkårResultatHolder = aktivitetHolder;
        this.andreLivsoppholdsytelserResultatHolder = livsoppholdHolder;
        this.bostedsvilkårResultatHolder = bostedHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Optional<BistandsvilkårResultatHolder> getBistandsvilkårResultatHolder() {
        return Optional.ofNullable(bistandsvilkårResultatHolder);
    }

    public Optional<AktivitetsvilkårResultatHolder> getAktivitetsvilkårResultatHolder() {
        return Optional.ofNullable(aktivitetsvilkårResultatHolder);
    }

    public List<AktivitetsvilkårResultatPeriode> hentAktivitetsvilkårResultatPerioder() {
        return getAktivitetsvilkårResultatHolder().map(AktivitetsvilkårResultatHolder::getVurderinger).orElse(List.of());
    }

    public List<BistandsvilkårResultatPeriode> hentBistandsvilkårResultatPerioder() {
        return getBistandsvilkårResultatHolder().map(BistandsvilkårResultatHolder::getVurderinger)
            .orElseThrow(() -> new IllegalStateException("Fant ikke BistandsvilkårResultatPerioder"));
    }

    public LocalDateTimeline<VilkårsvurderingResultat> hentBistandTidslinje() {
        return new LocalDateTimeline<>(getBistandsvilkårResultatHolder().map(BistandsvilkårResultatHolder::getVurderinger).orElse(List.of()).stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v.tilVilkårsvurderingResultat()))
            .toList());
    }

    public Optional<AndreLivsoppholdsytelserResultatHolder> getAndreLivsoppholdsytelserResultatHolder() {
        return Optional.ofNullable(andreLivsoppholdsytelserResultatHolder);
    }

    public List<AndreLivsoppholdsytelserResultatPeriode> hentAndreLivsoppholdsytelserResultatPerioder() {
        return getAndreLivsoppholdsytelserResultatHolder().map(AndreLivsoppholdsytelserResultatHolder::getVurderinger)
            .orElseThrow(() -> new IllegalStateException("Fant ikke AndreLivsoppholdsytelserResultatPerioder"));
    }

    public LocalDateTimeline<VilkårsvurderingResultat> hentLivsoppholdTidslinje() {
        return new LocalDateTimeline<>(getAndreLivsoppholdsytelserResultatHolder().map(AndreLivsoppholdsytelserResultatHolder::getVurderinger).orElse(List.of()).stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v.tilVilkårsvurderingResultat()))
            .toList());
    }

    Optional<BostedsvilkårResultatHolder> getBostedsvilkårResultatHolder() {
        return Optional.ofNullable(bostedsvilkårResultatHolder);
    }

    public List<BostedsvilkårResultatPeriode> hentBostedsvilkårResultatPerioder() {
        return getBostedsvilkårResultatHolder().map(BostedsvilkårResultatHolder::getVurderinger)
            .orElseThrow(() -> new IllegalStateException("Fant ikke BostedsvilkårResultatPerioder"));
    }

    public LocalDateTimeline<BostedsvilkårResultatPeriode> hentBostedTidslinje() {
        return new LocalDateTimeline<>(getBostedsvilkårResultatHolder().map(BostedsvilkårResultatHolder::getVurderinger).orElse(List.of()).stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        //FIXME denne kaller videre på holderne, som IKKE har implementert egne equals-metoder. Enten implementer i holderne også, eller gjør noe annet her
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AktivitetspengerInngangsvilkårResultatGrunnlag that = (AktivitetspengerInngangsvilkårResultatGrunnlag) o;
        return Objects.equals(bistandsvilkårResultatHolder, that.bistandsvilkårResultatHolder)
            && Objects.equals(aktivitetsvilkårResultatHolder, that.aktivitetsvilkårResultatHolder)
            && Objects.equals(andreLivsoppholdsytelserResultatHolder, that.andreLivsoppholdsytelserResultatHolder)
            && Objects.equals(bostedsvilkårResultatHolder, that.bostedsvilkårResultatHolder);
    }

    @Override
    public int hashCode() {
        //FIXME denne kaller videre på holderne, som IKKE har implementert egne hashCode-metoder. Enten implementer i holderne også, eller gjør noe annet her
        return Objects.hash(bistandsvilkårResultatHolder, aktivitetsvilkårResultatHolder, andreLivsoppholdsytelserResultatHolder, bostedsvilkårResultatHolder);
    }
}
