package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;
import no.nav.vedtak.konfig.KonfigVerdi;

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
    private Boolean toggletVilkårsperioder;

    protected FrisinnBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public FrisinnBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                    BeregningTjeneste kalkulusTjeneste,
                                    FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                    UttakRepository uttakRepository,
                                    @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "false") Boolean toggletVilkårsperioder) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.uttakRepository = uttakRepository;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (toggletVilkårsperioder) {
            var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse.fra(behandling));

            BeregningsresultatEntitet beregningsresultat = beregningsgrunnlag.isEmpty()
                ? BeregningsresultatEntitet.builder().medRegelInput("{}").medRegelSporing("{}").build() // tomt beregningresultat
                : fastsettBeregningsresultat(behandlingId, behandling, beregningsgrunnlag);

            // Verifiser beregningsresultat
            BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

            // Beregner ikke feriepenger for frisinn

            // Lagre beregningsresultat
            beregningsresultatRepository.lagre(behandling, beregningsresultat);
        } else {
            var originalBehandlingId = behandling.getOriginalBehandlingId();

            var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse.fra(behandling));

            BeregningsresultatEntitet beregningsresultat = beregningsgrunnlag.isEmpty()
                ? BeregningsresultatEntitet.builder().medRegelInput("{}").medRegelSporing("{}").build() // tomt beregningresultat
                : fastsettBeregningsresultat(behandlingId, behandling, beregningsgrunnlag);

            Boolean erNySøknadsperiode = originalBehandlingId.map(b -> erNySøknadperiode(behandling, b)).orElse(false);
            if (erNySøknadsperiode) {
                beregningsresultat = mergeMedOrigBeregningsresultat(beregningsresultat, behandlingId, originalBehandlingId);
            }

            // Verifiser beregningsresultat
            BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);
            // Beregner ikke feriepenger for frisinn

            // Lagre beregningsresultat
            beregningsresultatRepository.lagre(behandling, beregningsresultat);
        }


        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /**
     * Ved ny søknadsperiode mappes kun siste periode av {@link Beregningsgrunnlag} i Frisinn inn i
     * {@link BeregningsresultatEntitet}. De foregående periodene kopieres fra forrige behandlings
     * {@link BeregningsresultatEntitet}
     */
    private BeregningsresultatEntitet mergeMedOrigBeregningsresultat(BeregningsresultatEntitet beregningsresultat, Long behandlingId, Optional<Long> originalBehandlingId) {
        Behandling origBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());
        Optional<BeregningsresultatEntitet> origBeregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(origBehandling.getId());

        DatoIntervallEntitet sisteSøknadsperiode = uttakRepository.hentGrunnlag(behandlingId)
            .map(UttakGrunnlag::getOppgittSøknadsperioder)
            .map(Søknadsperioder::getMaksPeriode)
            .orElseThrow(() -> new IllegalStateException("Forventer uttaksgrunnlag"));

        return MapBeregningsresultat.mapResultatFraForrige(beregningsresultat, origBeregningsresultat, sisteSøknadsperiode);
    }

    private BeregningsresultatEntitet fastsettBeregningsresultat(Long behandlingId, Behandling behandling, List<Beregningsgrunnlag> beregningsgrunnlag) {
        UttakAktivitet fastsattUttak = uttakRepository.hentFastsattUttak(behandlingId);
        UttakResultat uttakResultat = MapUttakFrisinnTilRegel.map(fastsattUttak, behandling.getFagsakYtelseType());

        // Trim/juster beregningsgrunnlag før fastsetting (regelkall)
        beregningsgrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlag(beregningsgrunnlag);

        return fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttakResultat);
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }

    private boolean erNySøknadperiode(Behandling revurdering, Long origBehandlingId) {
        var nyttUttak = uttakRepository.hentFastsattUttak(revurdering.getId());
        var origUttak = uttakRepository.hentFastsattUttak(origBehandlingId);

        List<Periode> nyeSøknadsperioder = FrisinnSøknadsperiodeMapper.map(nyttUttak);
        List<Periode> origSøknadsperioder = FrisinnSøknadsperiodeMapper.map(origUttak);

        return nyeSøknadsperioder.size() > origSøknadsperioder.size();
    }

}
