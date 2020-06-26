package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
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
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "BERYT")
@BehandlingTypeRef
@ApplicationScoped
public class FrisinnBeregneYtelseSteg implements BeregneYtelseSteg {

    private static final LocalDate STP_FRISINN = LocalDate.of(2020, 3, 1);
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private UttakRepository uttakRepository;
    private Boolean skalBenytteTidligereResultat;
    private Boolean toggletVilkårsperioder;

    protected FrisinnBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public FrisinnBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                    BeregningTjeneste kalkulusTjeneste,
                                    FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                    UttakRepository uttakRepository,
                                    @Any Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjeneste,
                                    @KonfigVerdi(value = "SKAL_BENYTTE_RESULTAT_FRA_TIDLIGERE_BEHANDLING", defaultVerdi = "false") Boolean skalBenytteTidligereResultat,
                                    @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "true") Boolean toggletVilkårsperioder) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.uttakRepository = uttakRepository;
        this.skalBenytteTidligereResultat = skalBenytteTidligereResultat;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (toggletVilkårsperioder) {
            var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioder(BehandlingReferanse.fra(behandling));

            if (!beregningsgrunnlag.isEmpty()) {
                UttakAktivitet fastsattUttak = uttakRepository.hentFastsattUttak(behandlingId);
                UttakResultat uttakResultat = MapUttakFrisinnTilRegel.map(fastsattUttak, behandling.getFagsakYtelseType());

                // Kalle regeltjeneste
                Optional<Beregningsgrunnlag> mappetBeregningsgrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlag(beregningsgrunnlag);
                beregningsgrunnlag = mappetBeregningsgrunnlag.map(List::of).orElse(Collections.emptyList());

                var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttakResultat);

                // Verifiser beregningsresultat
                BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

                // Beregner ikke feriepenger for frisinn

                // Lagre beregningsresultat
                beregningsresultatRepository.lagre(behandling, beregningsresultat);
            } else {
                beregningsresultatRepository.lagre(behandling, BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build());
            }
        } else {
            var originalBehandling = behandling.getOriginalBehandling();

            var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsatt(BehandlingReferanse.fra(behandling), STP_FRISINN);
            var beregningsgrunnlagOriginalBehandling = originalBehandling.flatMap(b -> kalkulusTjeneste.hentEksaktFastsatt(BehandlingReferanse.fra(b), STP_FRISINN));

            if (beregningsgrunnlag.isPresent()) {
                UttakAktivitet fastsattUttak = uttakRepository.hentFastsattUttak(behandlingId);
                UttakResultat uttakResultat = MapUttakFrisinnTilRegel.map(fastsattUttak, behandling.getFagsakYtelseType());

                DatoIntervallEntitet sisteSøknadsperiode = uttakRepository.hentGrunnlag(behandlingId).map(UttakGrunnlag::getOppgittSøknadsperioder).map(Søknadsperioder::getMaksPeriode)
                    .orElseThrow(() -> new IllegalStateException("Forventer uttaksgrunnlag"));
                Boolean erNySøknadsperiode = originalBehandling.map(b -> erNySøknadperiode(behandling, b)).orElse(false);


                // Kalle regeltjeneste
                if (!erNySøknadsperiode) {
                    beregningsgrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlag(beregningsgrunnlag.get(), Optional.empty(), sisteSøknadsperiode, false);
                } else {
                    beregningsgrunnlag = mapForAlleTidligerePerioder(behandling, sisteSøknadsperiode);
                }

                var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag.map(List::of).orElse(Collections.emptyList()), uttakResultat);

                // Verifiser beregningsresultat
                BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

                // Beregner ikke feriepenger for frisinn

                // Lagre beregningsresultat
                beregningsresultatRepository.lagre(behandling, beregningsresultat);
                beregningsresultatRepository.lagre(behandling, BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build());
            }
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }

    private Optional<Beregningsgrunnlag> mapForAlleTidligerePerioder(Behandling behandling, DatoIntervallEntitet sisteSøknadsperiode) {
        Stream<Behandling> sisteBehandlingPrPeriode = flatten(behandling);
        Iterator<Behandling> iterator = sisteBehandlingPrPeriode.iterator();
        Behandling sisteBehandling = iterator.next();
        Optional<Beregningsgrunnlag> beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsatt(BehandlingReferanse.fra(sisteBehandling), STP_FRISINN);

        while (iterator.hasNext()) {
            Behandling neste = iterator.next();
            var bgNesteBehandling = kalkulusTjeneste.hentEksaktFastsatt(BehandlingReferanse.fra(neste), STP_FRISINN);
            if (beregningsgrunnlag.isPresent()) {
                beregningsgrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlag(beregningsgrunnlag.get(), bgNesteBehandling, sisteSøknadsperiode, skalBenytteTidligereResultat);
            }
            sisteSøknadsperiode = uttakRepository.hentGrunnlag(neste.getId()).map(UttakGrunnlag::getOppgittSøknadsperioder).map(Søknadsperioder::getMaksPeriode)
                .orElseThrow(() -> new IllegalStateException("Forventer uttaksgrunnlag"));
        }
        return beregningsgrunnlag;
    }


    private Stream<Behandling> flatten(Behandling behandling) {
        Boolean erNySøknadsperiode = behandling.getOriginalBehandling().map(b -> erNySøknadperiode(behandling, b)).orElse(false);
        Stream<Behandling> flattened = behandling.getOriginalBehandling().stream().flatMap(this::flatten);
        if (erNySøknadsperiode) {
            return Stream.concat(Stream.of(behandling), flattened);
        } else {
            return Stream.concat(Stream.of(behandling), flattened.skip(1));
        }
    }

    private boolean erNySøknadperiode(Behandling revurdering, Behandling origBehandling) {
        var nyttUttak = uttakRepository.hentFastsattUttak(revurdering.getId());
        var origUttak = uttakRepository.hentFastsattUttak(origBehandling.getId());

        return nyttUttak.getPerioder().size() > origUttak.getPerioder().size();
    }

}
