package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.foreldrepenger.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

/** Felles steg for å beregne tilkjent ytelse */

@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class BeregneYtelseStegImpl implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected BeregneYtelseStegImpl() {
        // for proxy
    }

    @Inject
    public BeregneYtelseStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                 BeregningTjeneste kalkulusTjeneste,
                                 UttakInputTjeneste uttakInputTjeneste,
                                 FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var input = uttakInputTjeneste.lagInput(ref);

        Beregningsgrunnlag beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsatt(behandlingId);

        // Kalle regeltjeneste
        BeregningsresultatEntitet beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, input);

        // Verifiser beregningsresultat
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        feriepengerTjeneste.beregnFeriepenger(beregningsresultat);

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }
}
