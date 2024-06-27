package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FeriepengeBeregner;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped

//for grenser, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-10

public class OmsorgspengerBeregnFeriepenger implements BeregnFeriepengerTjeneste {

    private static final int ANTALL_DAGER_FERIPENGER = 48;
    private static final boolean FERIEOPPTJENING_HELG = true;
    private static final boolean UBEGRENSET_DAGER_VED_REFUSJON = true;
    private FeriepengeBeregner feriepengeBeregner;
    private VilkårTjeneste vilkårTjeneste;

    public OmsorgspengerBeregnFeriepenger() {
    }

    @Inject
    public OmsorgspengerBeregnFeriepenger(FeriepengeBeregner feriepengeBeregner, VilkårTjeneste vilkårTjeneste) {
        this.feriepengeBeregner = feriepengeBeregner;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public void beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        //ingen saker andre påvirker
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = LocalDateTimeline.empty();
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = null;

        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat,
            påvirkendeSaker, infotrygdFeriepengegrunnlag,
            ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON,
            Collections.emptyList(),
            finnSkjæringstidspunkter(ref)); // Bruker mottar aldri omsorgspenger for dagpenger
        feriepengeBeregner.beregnFeriepenger(beregningsresultat, regelModell);
    }

    @Override
    public FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat) {
        //ingen saker andre påvirker
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeSaker = LocalDateTimeline.empty();
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = null;

        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat,
            påvirkendeSaker,
            infotrygdFeriepengegrunnlag,
            ANTALL_DAGER_FERIPENGER, FERIEOPPTJENING_HELG, UBEGRENSET_DAGER_VED_REFUSJON,
            Collections.emptyList(),
            finnSkjæringstidspunkter(ref)); // Bruker mottar aldri omsorgspenger for dagpenger
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


}
