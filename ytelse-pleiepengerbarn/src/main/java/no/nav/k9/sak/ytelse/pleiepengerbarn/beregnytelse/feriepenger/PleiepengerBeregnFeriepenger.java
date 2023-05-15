package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FeriepengeBeregner;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
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

    private HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste;
    private Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester;

    PleiepengerBeregnFeriepenger() {
        //for CDI proxy
    }

    @Inject
    public PleiepengerBeregnFeriepenger(HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste, @Any Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester) {
        this.hentFeriepengeAndelerTjeneste = hentFeriepengeAndelerTjeneste;
        this.feriepengepåvirkendeFagsakerTjenester = feriepengepåvirkendeFagsakerTjenester;
    }

    @Override
    public void beregnFeriepenger(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkedeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON);
        FeriepengeBeregner.beregnFeriepenger(beregningsresultat, regelModell);
    }

    @Override
    public FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse behandling, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = finnPåvirkedeSaker(behandling);
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON);
        return FeriepengeBeregner.beregnFeriepengerOppsummering(regelModell);
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkedeSaker(BehandlingReferanse behandling) {
        var feriepengepåvirkendeFagsakerTjeneste = FinnFeriepengepåvirkendeFagsakerTjeneste.finnTjeneste(feriepengepåvirkendeFagsakerTjenester, behandling.getFagsakYtelseType());
        Set<Fagsak> påvirkendeFagsaker = feriepengepåvirkendeFagsakerTjeneste.finnSakerSomPåvirkerFeriepengerFor(behandling);
        return hentFeriepengeAndelerTjeneste.finnAndelerSomKanGiFeriepenger(påvirkendeFagsaker);
    }

}
