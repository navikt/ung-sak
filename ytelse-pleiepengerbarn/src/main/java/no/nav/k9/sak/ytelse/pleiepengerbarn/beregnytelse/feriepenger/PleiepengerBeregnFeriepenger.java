package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.DAGPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
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

    private Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    PleiepengerBeregnFeriepenger() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerBeregnFeriepenger(@Any Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester,
                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.feriepengepåvirkendeFagsakerTjenester = feriepengepåvirkendeFagsakerTjenester;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public void beregnFeriepenger(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkendeSaker(behandling);
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, infotrygdFeriepengegrunnlag, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON, finnPerioderMedDagpenger(behandling));
        FeriepengeBeregner.beregnFeriepenger(beregningsresultat, regelModell);
    }

    @Override
    public FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkendeSaker(behandling);
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, infotrygdFeriepengegrunnlag, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON, finnPerioderMedDagpenger(behandling));
        return FeriepengeBeregner.beregnFeriepengerOppsummering(regelModell);
    }

    private List<DagpengerPeriode> finnPerioderMedDagpenger(BehandlingReferanse ref) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingUuid());
        var alleYtelser = iayGrunnlag.getAktørYtelseFraRegister(ref.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(List.of());
        var perioder = new ArrayList<DagpengerPeriode>();
        perioder.addAll(finnDagpengerFraMeldekort(alleYtelser));
        return perioder;
    }

    private static List<DagpengerPeriode> finnDagpengerFraMeldekort(Collection<Ytelse> alleYtelser) {
        var meldekortPerioder = alleYtelser.stream().filter(yt -> DAGPENGER.equals(yt.getYtelseType())).flatMap(yt -> yt.getYtelseAnvist().stream()).toList();
        var meldekortDagpengeperioder = meldekortPerioder.stream().map(mk -> new DagpengerPeriode(DagpengerKilde.MELDEKORT, mk.getAnvistFOM(), mk.getAnvistTOM())).toList();
        return meldekortDagpengeperioder;
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
