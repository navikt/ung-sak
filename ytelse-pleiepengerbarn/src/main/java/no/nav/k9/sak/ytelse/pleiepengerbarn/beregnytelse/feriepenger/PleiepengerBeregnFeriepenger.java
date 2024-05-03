package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FeriepengeBeregner;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.*;

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

    private List<LocalDateInterval> finnPerioderMedDagpenger(BehandlingReferanse ref) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingUuid());
        var alleYtelser = iayGrunnlag.getAktørYtelseFraRegister(ref.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(List.of());
        var meldekortPerioder = alleYtelser.stream().filter(yt -> DAGPENGER.equals(yt.getYtelseType())).flatMap(yt -> yt.getYtelseAnvist().stream()).toList();
        return meldekortPerioder.stream().map(mk -> new LocalDateInterval(mk.getAnvistFOM(), mk.getAnvistTOM())).toList();
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeSaker(BehandlingReferanse behandling) {
        return finnFeriepengepåvirkendeFagsakerTjeneste(behandling).finnPåvirkedeSaker(behandling);
    }

    private InfotrygdFeriepengegrunnlag finnInfotrygdFeriepengegrunnlagForPåvirkendeSaker(BehandlingReferanse behandlingReferanse){
        return finnFeriepengepåvirkendeFagsakerTjeneste(behandlingReferanse).finnInfotrygdFeriepengegrunnlag(behandlingReferanse);
    }

    private FinnFeriepengepåvirkendeFagsakerTjeneste finnFeriepengepåvirkendeFagsakerTjeneste(BehandlingReferanse behandling) {
        return FinnFeriepengepåvirkendeFagsakerTjeneste.finnTjeneste(feriepengepåvirkendeFagsakerTjenester, behandling.getFagsakYtelseType());
    }

}
