package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;


@FagsakYtelseTypeRef("OMP")
@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@ApplicationScoped
public class OmsorgspengerBeregneYtelseSteg implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    protected OmsorgspengerBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public OmsorgspengerBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                          BeregningTjeneste kalkulusTjeneste,
                                          ÅrskvantumTjeneste årskvantumTjeneste,
                                          FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioder(ref);

        var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumUttak(ref);
        var uttaksresultat = new UttakResultat(ref.getFagsakYtelseType(), new MapFraÅrskvantumResultat().mapFra(årskvantumResultat));

        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttaksresultat);

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
