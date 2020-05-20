package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

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
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@ApplicationScoped
public class FrisinnBeregneYtelseSteg implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private UttakRepository uttakRepository;

    protected FrisinnBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public FrisinnBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                    BeregningTjeneste kalkulusTjeneste,
                                    FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                    UttakRepository uttakRepository,
                                    @Any Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioder(BehandlingReferanse.fra(behandling));

        UttakAktivitet fastsattUttak = uttakRepository.hentFastsattUttak(behandlingId);
        UttakResultat uttakResultat = MapUttakFrisinnTilRegel.map(fastsattUttak, behandling.getFagsakYtelseType());

        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttakResultat);

        // Verifiser beregningsresultat
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

        // Beregner ikke feriepenger for frisinn

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
