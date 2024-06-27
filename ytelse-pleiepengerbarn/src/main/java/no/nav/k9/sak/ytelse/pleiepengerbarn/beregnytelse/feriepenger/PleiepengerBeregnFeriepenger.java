package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.DAGPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FORELDREPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.SYKEPENGER;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FeriepengeBeregner;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerKilde;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped

//for grenser, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-11

public class PleiepengerBeregnFeriepenger implements BeregnFeriepengerTjeneste {
    private static final int ANTALL_DAGER_FERIPENGER = 60;
    private static final boolean FERIEOPPTJENING_HELG = false;
    private static final boolean UBEGRENSET_DAGER_VED_REFUSJON = false;
    public static final Set<FagsakYtelseType> YTELSER_FOR_849 = Set.of(PLEIEPENGER_NÆRSTÅENDE, PLEIEPENGER_SYKT_BARN, FORELDREPENGER, OPPLÆRINGSPENGER, SYKEPENGER);

    private Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårTjeneste vilkårTjeneste;
    private FeriepengeBeregner feriepengeBeregner;

    PleiepengerBeregnFeriepenger() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerBeregnFeriepenger(@Any Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                        VilkårTjeneste vilkårTjeneste,
                                        FeriepengeBeregner feriepengeBeregner) {
        this.feriepengepåvirkendeFagsakerTjenester = feriepengepåvirkendeFagsakerTjenester;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.feriepengeBeregner = feriepengeBeregner;
    }

    @Override
    public void beregnFeriepenger(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkendeSaker(behandling);
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(
            beregningsresultat,
            påvirkendeSaker,
            infotrygdFeriepengegrunnlag,
            ANTALL_DAGER_FERIPENGER,
            FERIEOPPTJENING_HELG,
            UBEGRENSET_DAGER_VED_REFUSJON,
            finnPerioderMedDagpenger(behandling),
            finnSkjæringstidspunkter(behandling));
        feriepengeBeregner.beregnFeriepenger(beregningsresultat, regelModell);
    }

    @Override
    public FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkendeSaker(behandling);
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(
            beregningsresultat,
            påvirkendeSaker,
            infotrygdFeriepengegrunnlag,
            ANTALL_DAGER_FERIPENGER,
            FERIEOPPTJENING_HELG,
            UBEGRENSET_DAGER_VED_REFUSJON,
            finnPerioderMedDagpenger(behandling),
            finnSkjæringstidspunkter(behandling));
        return feriepengeBeregner.beregnFeriepengerOppsummering(regelModell);
    }

    private Set<LocalDate> finnSkjæringstidspunkter(BehandlingReferanse behandling) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(behandling.getBehandlingId());
        var bgVilkåret = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return bgVilkåret.map(Vilkår::getPerioder)
            .stream()
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getFom)
            .collect(Collectors.toSet());
    }

    private List<DagpengerPeriode> finnPerioderMedDagpenger(BehandlingReferanse ref) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingUuid());
        var alleYtelser = iayGrunnlag.getAktørYtelseFraRegister(ref.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(List.of());
        var perioder = new ArrayList<DagpengerPeriode>();
        perioder.addAll(finnDagpengerFraMeldekort(alleYtelser));
        perioder.addAll(finnDagpengerFraYtelser(alleYtelser));
        return perioder;
    }

    private List<DagpengerPeriode> finnDagpengerFraYtelser(Collection<Ytelse> alleYtelser) {
        var perioderFraYtelser = alleYtelser.stream().filter(y -> YTELSER_FOR_849.contains(y.getYtelseType()))
            .filter(y -> y.getYtelseAnvist().stream().min(Comparator.comparing(YtelseAnvist::getAnvistFOM))
                .filter(ya -> ya.getYtelseAnvistAndeler().stream().anyMatch(a -> a.getInntektskategori().equals(Inntektskategori.DAGPENGER))).isPresent())
            .map(y -> new DagpengerPeriode(mapTilKilde(y.getYtelseType()), y.getPeriode().getFomDato(), y.getPeriode().getTomDato()))
            .toList();
        return perioderFraYtelser;
    }

    private DagpengerKilde mapTilKilde(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case OPPLÆRINGSPENGER:
                return DagpengerKilde.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN:
                return DagpengerKilde.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE:
                return DagpengerKilde.PLEIEPENGER_NÆRSTÅENDE;
            case FORELDREPENGER:
                return DagpengerKilde.FORELDREPENGER;
            case SYKEPENGER:
                return DagpengerKilde.SYKEPENGER;
            default:
                throw new IllegalArgumentException("Kun ytelser som kan beregnes etter §8-49 skal mappes inn");
        }
    }

    private static List<DagpengerPeriode> finnDagpengerFraMeldekort(Collection<Ytelse> alleYtelser) {
        var meldekortPerioder = finnMeldekortMedUtbetaling(alleYtelser);
        var meldekortDagpengeperioder = meldekortPerioder.stream().map(mk -> new DagpengerPeriode(DagpengerKilde.MELDEKORT, mk.getAnvistFOM(), mk.getAnvistTOM())).toList();
        return meldekortDagpengeperioder;
    }

    private static List<YtelseAnvist> finnMeldekortMedUtbetaling(Collection<Ytelse> alleYtelser) {
        return alleYtelser.stream().filter(yt -> DAGPENGER.equals(yt.getYtelseType())).flatMap(yt -> yt.getYtelseAnvist().stream())
            .filter(ya -> ya.getUtbetalingsgradProsent().map(g -> g.getVerdi().compareTo(BigDecimal.ZERO) > 0).orElse(false)).toList();
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeSaker(BehandlingReferanse behandling) {
        return finnFeriepengepåvirkendeFagsakerTjeneste(behandling).finnPåvirkedeSaker(behandling);
    }

    private InfotrygdFeriepengegrunnlag finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(BehandlingReferanse behandlingReferanse) {
        return finnFeriepengepåvirkendeFagsakerTjeneste(behandlingReferanse).finnInfotrygdFeriepengegrunnlag(behandlingReferanse);
    }

    private FinnFeriepengepåvirkendeFagsakerTjeneste finnFeriepengepåvirkendeFagsakerTjeneste(BehandlingReferanse behandling) {
        return FinnFeriepengepåvirkendeFagsakerTjeneste.finnTjeneste(feriepengepåvirkendeFagsakerTjenester, behandling.getFagsakYtelseType());
    }

}
