package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapInntektskategoriFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.HarDekningsgrad;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Dekningsgrad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SammenligningsGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

@ApplicationScoped
public class MapBeregningsgrunnlagFraVLTilRegel {

    private Instance<GrunnbeløpTjeneste> grunnbeløpTjenesteInstances;
    private MapInntektsgrunnlagVLTilRegel mapInntektsgrunnlagVLTilRegel;
    private final Map<SammenligningsgrunnlagType, AktivitetStatus> SAMMENLIGNINGSGRUNNLAGTYPE_AKTIVITETSTATUS_MAP = Map.of(
        SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN, AktivitetStatus.ATFL_SN,
        SammenligningsgrunnlagType.SAMMENLIGNING_AT, AktivitetStatus.AT,
        SammenligningsgrunnlagType.SAMMENLIGNING_FL, AktivitetStatus.FL,
        SammenligningsgrunnlagType.SAMMENLIGNING_SN, AktivitetStatus.SN
    );

    MapBeregningsgrunnlagFraVLTilRegel() {
        // CDI
    }

    @Inject
    public MapBeregningsgrunnlagFraVLTilRegel(@Any Instance<GrunnbeløpTjeneste> grunnbeløpTjenesteInstances,
                                              MapInntektsgrunnlagVLTilRegel mapInntektsgrunnlagVLTilRegel) {
        this.grunnbeløpTjenesteInstances = grunnbeløpTjenesteInstances;
        this.mapInntektsgrunnlagVLTilRegel = mapInntektsgrunnlagVLTilRegel;
    }

    private static List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak> mapPeriodeÅrsak(List<BeregningsgrunnlagPeriodeÅrsak> beregningsgrunnlagPeriodeÅrsaker) {
        if (beregningsgrunnlagPeriodeÅrsaker.isEmpty()) {
            return Collections.emptyList();
        }
        List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak> periodeÅrsakerMapped = new ArrayList<>();
        beregningsgrunnlagPeriodeÅrsaker.forEach(bgPeriodeÅrsak -> {
            if (!PeriodeÅrsak.UDEFINERT.equals(bgPeriodeÅrsak.getPeriodeÅrsak())) {
                try {
                    periodeÅrsakerMapped.add(
                        no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.valueOf(bgPeriodeÅrsak.getPeriodeÅrsak().getKode()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Ukjent PeriodeÅrsak: (" + bgPeriodeÅrsak.getPeriodeÅrsak().getKode() + ").", e);
                }
            }
        });
        return periodeÅrsakerMapped;
    }

    public List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode> mapTilFordelingsregel(BehandlingReferanse referanse,
                                                                                                                                            BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        Objects.requireNonNull(referanse, "BehandlingReferanse kan ikke være null!");
        Objects.requireNonNull(beregningsgrunnlagEntitet, "Beregningsgrunnlag kan ikke være null!");
        return mapBeregningsgrunnlagPerioder(beregningsgrunnlagEntitet);
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag map(BeregningsgrunnlagInput input,
                                                                                                             BeregningsgrunnlagGrunnlagEntitet oppdatertGrunnlag) {
        var ref = input.getBehandlingReferanse();
        Objects.requireNonNull(ref, "BehandlingReferanse kan ikke være null!");
        Objects.requireNonNull(oppdatertGrunnlag, "BeregningsgrunnlagGrunnlag kan ikke være null");
        BeregningsgrunnlagEntitet beregningsgrunnlag = oppdatertGrunnlag.getBeregningsgrunnlag().orElse(null);
        Objects.requireNonNull(beregningsgrunnlag, "Beregningsgrunnlag kan ikke være null!");
        var inntektsmeldinger = input.getInntektsmeldinger();

        List<AktivitetStatusMedHjemmel> aktivitetStatuser = beregningsgrunnlag.getAktivitetStatuser().stream()
            .map(this::mapVLAktivitetStatusMedHjemmel)
            .sorted()
            .collect(Collectors.toList());

        Inntektsgrunnlag inntektsgrunnlag = mapInntektsgrunnlagVLTilRegel.map(ref, inntektsmeldinger, beregningsgrunnlag.getSkjæringstidspunkt(),
            input.getIayGrunnlag());
        List<BeregningsgrunnlagPeriode> perioder = mapBeregningsgrunnlagPerioder(beregningsgrunnlag);
        // Sammenligningsgrunnlaget blir alltid satt inne i regel
        Map<AktivitetStatus, SammenligningsGrunnlag> sammenligningsgrunnlagMap = mapSammenligningsgrunnlagPrStatus(beregningsgrunnlag);
        SammenligningsGrunnlag sammenligningsgrunnlag = beregningsgrunnlag.getSammenligningsgrunnlag() != null
            ? mapSammenligningsGrunnlag(beregningsgrunnlag.getSammenligningsgrunnlag())
            : null;
            
        Dekningsgrad dekningsgrad = (input.getYtelsespesifiktGrunnlag() instanceof HarDekningsgrad)
            ? finnDekningsgrad((HarDekningsgrad) input.getYtelsespesifiktGrunnlag())
            : Dekningsgrad.DEKNINGSGRAD_100; // 100% - dvs. ingen fordeling

        LocalDate skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();

        GrunnbeløpTjeneste grunnbeløpTjeneste = FagsakYtelseTypeRef.Lookup.find(grunnbeløpTjenesteInstances, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Finner ikke implementasjon for håndtering av grunnbeløp for BehandlingReferanse " + ref));

        List<Grunnbeløp> grunnbeløpList = grunnbeløpTjeneste.mapGrunnbeløpSatser();
        boolean erMilitærIOpptjeningsperioden = harHattMilitærIOpptjeningsperioden(oppdatertGrunnlag.getGjeldendeAktiviteter());
        Integer antallGrunnbeløpMilitærHarKravPå = grunnbeløpTjeneste.finnAntallGrunnbeløpMilitærHarKravPå();

        var builder = no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag.builder();
        sammenligningsgrunnlagMap.forEach(builder::medSammenligningsgrunnlagPrStatus);
        return builder
            .medInntektsgrunnlag(inntektsgrunnlag)
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktivitetStatuser(aktivitetStatuser)
            .medBeregningsgrunnlagPerioder(perioder)
            .medSammenligningsgrunnlag(sammenligningsgrunnlag)
            .medDekningsgrad(dekningsgrad)
            .medGrunnbeløp(beregningsgrunnlag.getGrunnbeløp().getVerdi())
            .medGrunnbeløpSatser(grunnbeløpList)
            .medMilitærIOpptjeningsperioden(erMilitærIOpptjeningsperioden)
            .medAntallGrunnbeløpMilitærHarKravPå(antallGrunnbeløpMilitærHarKravPå)
            .build();
    }

    private Dekningsgrad finnDekningsgrad(HarDekningsgrad input) {
        return Dekningsgrad.fra(input.getDekningsgrad());
    }

    private AktivitetStatusMedHjemmel mapVLAktivitetStatusMedHjemmel(final BeregningsgrunnlagAktivitetStatus vlBGAktivitetStatus) {
        BeregningsgrunnlagHjemmel hjemmel = null;
        if (!Hjemmel.UDEFINERT.equals(vlBGAktivitetStatus.getHjemmel())) {
            try {
                hjemmel = BeregningsgrunnlagHjemmel.valueOf(vlBGAktivitetStatus.getHjemmel().getKode());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Ukjent Hjemmel: (" + vlBGAktivitetStatus.getHjemmel().getKode() + ").", e);
            }
        }
        AktivitetStatus as = mapVLAktivitetStatus(vlBGAktivitetStatus.getAktivitetStatus());
        return new AktivitetStatusMedHjemmel(as, hjemmel);
    }

    private AktivitetStatus mapVLAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus vlBGAktivitetStatus) {
        if (BeregningsgrunnlagUtil.erATFL(vlBGAktivitetStatus)) {
            return AktivitetStatus.ATFL;
        }

        try {
            return AktivitetStatus.valueOf(vlBGAktivitetStatus.getKode());
        } catch (IllegalArgumentException e) {
            if (BeregningsgrunnlagUtil.erATFL_SN(vlBGAktivitetStatus)) {
                return AktivitetStatus.ATFL_SN;
            }
            throw new IllegalStateException("Ukjent AktivitetStatus: (" + vlBGAktivitetStatus.getKode() + ").", e);
        }
    }

    private SammenligningsGrunnlag mapSammenligningsGrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag) {
        return SammenligningsGrunnlag.builder()
            .medSammenligningsperiode(new Periode(
                sammenligningsgrunnlag.getSammenligningsperiodeFom(),
                sammenligningsgrunnlag.getSammenligningsperiodeTom()))
            .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
            .medAvvikProsentFraPromille(sammenligningsgrunnlag.getAvvikPromille())
            .build();
    }

    private List<BeregningsgrunnlagPeriode> mapBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        List<BeregningsgrunnlagPeriode> perioder = new ArrayList<>();
        vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(vlBGPeriode -> {
            final BeregningsgrunnlagPeriode.Builder regelBGPeriode = BeregningsgrunnlagPeriode.builder()
                .medPeriode(Periode.of(vlBGPeriode.getBeregningsgrunnlagPeriodeFom(), vlBGPeriode.getBeregningsgrunnlagPeriodeTom()))
                .leggTilPeriodeÅrsaker(mapPeriodeÅrsak(vlBGPeriode.getBeregningsgrunnlagPeriodeÅrsaker()));

            List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus = mapVLBGPrStatus(vlBGPeriode);
            beregningsgrunnlagPrStatus.forEach(regelBGPeriode::medBeregningsgrunnlagPrStatus);
            perioder.add(regelBGPeriode.build());
        });

        return perioder;
    }

    private List<BeregningsgrunnlagPrStatus> mapVLBGPrStatus(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        List<BeregningsgrunnlagPrStatus> liste = new ArrayList<>();
        BeregningsgrunnlagPrStatus bgpsATFL = null;

        for (BeregningsgrunnlagPrStatusOgAndel vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            final AktivitetStatus regelAktivitetStatus = mapVLAktivitetStatus(vlBGPStatus.getAktivitetStatus());
            if (AktivitetStatus.ATFL.equals(regelAktivitetStatus) || AktivitetStatus.AT.equals(regelAktivitetStatus)) {
                if (bgpsATFL == null) { // Alle ATFL håndteres samtidig her
                    bgpsATFL = mapVLBGPStatusForATFL(vlBGPeriode, regelAktivitetStatus);
                    liste.add(bgpsATFL);
                }
            } else {
                BeregningsgrunnlagPrStatus bgps = mapVLBGPStatusForAlleAktivietetStatuser(vlBGPStatus);
                liste.add(bgps);
            }
        }
        return liste;
    }

    private Map<AktivitetStatus, SammenligningsGrunnlag> mapSammenligningsgrunnlagPrStatus(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        Map<AktivitetStatus, SammenligningsGrunnlag> sammenligningsGrunnlagMap = new HashMap<>();
        for (SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus : beregningsgrunnlagEntitet.getSammenligningsgrunnlagPrStatusListe()) {
            SammenligningsGrunnlag sammenligningsGrunnlag = SammenligningsGrunnlag.builder()
                .medSammenligningsperiode(new Periode(
                    sammenligningsgrunnlagPrStatus.getSammenligningsperiodeFom(),
                    sammenligningsgrunnlagPrStatus.getSammenligningsperiodeTom()))
                .medRapportertPrÅr(sammenligningsgrunnlagPrStatus.getRapportertPrÅr())
                .medAvvikProsentFraPromille(sammenligningsgrunnlagPrStatus.getAvvikPromille())
                .build();
            sammenligningsGrunnlagMap.put(SAMMENLIGNINGSGRUNNLAGTYPE_AKTIVITETSTATUS_MAP.get(sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType()),
                sammenligningsGrunnlag);
        }
        return sammenligningsGrunnlagMap;
    }

    // Ikke ATFL og TY, de har separat mapping
    private BeregningsgrunnlagPrStatus mapVLBGPStatusForAlleAktivietetStatuser(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        final AktivitetStatus regelAktivitetStatus = mapVLAktivitetStatus(vlBGPStatus.getAktivitetStatus());
        List<BigDecimal> pgi = (vlBGPStatus.getPgiSnitt() == null ? new ArrayList<>()
            : Arrays.asList(vlBGPStatus.getPgi1(), vlBGPStatus.getPgi2(), vlBGPStatus.getPgi3()));
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medBeregningsperiode(beregningsperiodeFor(vlBGPStatus))
            .medBeregnetPrÅr(vlBGPStatus.getBeregnetPrÅr())
            .medOverstyrtPrÅr(vlBGPStatus.getOverstyrtPrÅr())
            .medFordeltPrÅr(vlBGPStatus.getFordeltPrÅr())
            .medGjennomsnittligPGI(vlBGPStatus.getPgiSnitt())
            .medPGI(pgi)
            .medÅrsbeløpFraTilstøtendeYtelse(vlBGPStatus.getÅrsbeløpFraTilstøtendeYtelseVerdi())
            .medErNyIArbeidslivet(vlBGPStatus.getNyIArbeidslivet())
            .medAndelNr(vlBGPStatus.getAndelsnr())
            .medInntektskategori(MapInntektskategoriFraVLTilRegel.map(vlBGPStatus.getInntektskategori()))
            .medFastsattAvSaksbehandler(vlBGPStatus.getFastsattAvSaksbehandler())
            .medLagtTilAvSaksbehandler(vlBGPStatus.getLagtTilAvSaksbehandler())
            .medBesteberegningPrÅr(vlBGPStatus.getBesteberegningPrÅr())
            .medOrginalDagsatsFraTilstøtendeYtelse(vlBGPStatus.getOrginalDagsatsFraTilstøtendeYtelse())
            .build();
    }

    private Periode beregningsperiodeFor(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        if (vlBGPStatus.getBeregningsperiodeFom() == null && vlBGPStatus.getBeregningsperiodeTom() == null) {
            return null;
        }
        return Periode.of(vlBGPStatus.getBeregningsperiodeFom(), vlBGPStatus.getBeregningsperiodeTom());
    }

    // Felles mapping av alle statuser som mapper til ATFL
    private BeregningsgrunnlagPrStatus mapVLBGPStatusForATFL(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode,
                                                             AktivitetStatus regelAktivitetStatus) {

        BeregningsgrunnlagPrStatus.Builder regelBGPStatusATFL = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(regelAktivitetStatus)
            .medFlOgAtISammeOrganisasjon(
                vlBGPeriode.getBeregningsgrunnlag().getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON));

        for (BeregningsgrunnlagPrStatusOgAndel vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            if (regelAktivitetStatus.equals(mapVLAktivitetStatus(vlBGPStatus.getAktivitetStatus()))) {
                BeregningsgrunnlagPrArbeidsforhold regelArbeidsforhold = byggAndel(vlBGPStatus);
                regelBGPStatusATFL.medArbeidsforhold(regelArbeidsforhold);
            }
        }
        return regelBGPStatusATFL.build();
    }

    private BeregningsgrunnlagPrArbeidsforhold byggAndel(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        BeregningsgrunnlagPrArbeidsforhold.Builder builder = BeregningsgrunnlagPrArbeidsforhold.builder();
        builder
            .medInntektskategori(MapInntektskategoriFraVLTilRegel.map(vlBGPStatus.getInntektskategori()))
            .medBeregnetPrÅr(vlBGPStatus.getBeregnetPrÅr())
            .medBeregningsperiode(beregningsperiodeFor(vlBGPStatus))
            .medFastsattAvSaksbehandler(vlBGPStatus.getFastsattAvSaksbehandler())
            .medLagtTilAvSaksbehandler(vlBGPStatus.getLagtTilAvSaksbehandler())
            .medAndelNr(vlBGPStatus.getAndelsnr())
            .medOverstyrtPrÅr(vlBGPStatus.getOverstyrtPrÅr())
            .medFordeltPrÅr(vlBGPStatus.getFordeltPrÅr())
            .medArbeidsforhold(MapArbeidsforholdFraVLTilRegel.arbeidsforholdFor(vlBGPStatus));

        vlBGPStatus.getBgAndelArbeidsforhold().ifPresent(bga -> builder
            .medNaturalytelseBortfaltPrÅr(bga.getNaturalytelseBortfaltPrÅr().orElse(null))
            .medNaturalytelseTilkommetPrÅr(bga.getNaturalytelseTilkommetPrÅr().orElse(null))
            .medErTidsbegrensetArbeidsforhold(bga.getErTidsbegrensetArbeidsforhold())
            .medRefusjonskravPrÅr(bga.getRefusjonskravPrÅr()));

        return builder.build();
    }

    private boolean harHattMilitærIOpptjeningsperioden(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        Objects.requireNonNull(beregningAktivitetAggregat, "beregningAktivitetAggregat");
        return beregningAktivitetAggregat.getBeregningAktiviteter().stream()
            .map(BeregningAktivitetEntitet::getOpptjeningAktivitetType)
            .anyMatch(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE::equals);
    }
}
