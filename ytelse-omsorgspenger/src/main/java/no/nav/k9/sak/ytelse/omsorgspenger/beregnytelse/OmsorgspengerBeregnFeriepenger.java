package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FeriepengeBeregner;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@Dependent

//for grenser, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-10

public class OmsorgspengerBeregnFeriepenger implements BeregnFeriepengerTjeneste {

    private static final int ANTALL_DAGER_FERIPENGER = 48;
    private static final boolean FERIEOPPTJENING_HELG = true;
    private static final boolean UBEGRENSET_DAGER_VED_REFUSJON = true;

    @Override
    public void beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = LocalDateTimeline.empty();
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON);
        FeriepengeBeregner.beregnFeriepenger(beregningsresultat, regelModell);
    }

    @Override
    public FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = LocalDateTimeline.empty();
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, påvirkendeSaker, ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON);
        return FeriepengeBeregner.beregnFeriepengerOppsummering(regelModell);
    }


}
