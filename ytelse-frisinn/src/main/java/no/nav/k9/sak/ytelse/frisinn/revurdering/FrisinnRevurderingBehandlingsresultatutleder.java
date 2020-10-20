package no.nav.k9.sak.ytelse.frisinn.revurdering;

import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.BeregningsresultatEndring.GUNST;
import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.BeregningsresultatEndring.UGUNST;

import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
@BehandlingTypeRef("BT-004")
public class FrisinnRevurderingBehandlingsresultatutleder implements RevurderingBehandlingsresultatutleder {

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;
    private Boolean toggletVilkårsperioder;

    @Inject
    public FrisinnRevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                        BeregningsresultatRepository beregningsresultatRepository,
                                                        UttakRepository uttakRepository,
                                                        @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "false") Boolean toggletVilkårsperioder) {

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakRepository = uttakRepository;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }


    @Override
    public void bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());
        Long originalBehandlingId = revurdering.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Mangler originalBehandlingId for behandling: " + revurdering.getId()));
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        doBestemBehandlingsresultatForRevurdering(revurdering, originalBehandling);
    }

    private void doBestemBehandlingsresultatForRevurdering(Behandling revurdering, Behandling originalBehandling) {
        var beregningsvilkår = vilkårResultatRepository.hent(revurdering.getId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var erNySøknadsperiode = erNySøknadperiode(revurdering, originalBehandling);
        var erEndringIBeregning = erEndringIBeregning(revurdering, originalBehandling);
        var erHeltEllerDelvisInnvilget = erHeltEllerDelvisInnvilget(beregningsvilkår);

        // Oppdatering av Behandlingsresultat dersom betingelsene nedenfor inntreffer (ikke heldig å oppdatere verdi i ettertid)
        if (erHeltEllerDelvisInnvilget) {
            if (erNySøknadsperiode || erEndringIBeregning) {
                revurdering.setBehandlingResultatType(BehandlingResultatType.INNVILGET_ENDRING);
            } else {
                revurdering.setBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
            }
        }
    }

    private boolean erHeltEllerDelvisInnvilget(Vilkår beregningsvilkår) {
        if (toggletVilkårsperioder) {
            return beregningsvilkår.getPerioder().stream()
                .max(Comparator.comparing(p -> p.getPeriode().getFomDato()))
                .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
                .isPresent();
        }
        return beregningsvilkår.getPerioder().stream()
            .anyMatch(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT));
    }

    private boolean erEndringIBeregning(Behandling revurdering, Behandling originalBehandling) {
        UttakAktivitet orginaltUttak = uttakRepository.hentFastsattUttak(originalBehandling.getId());

        Optional<BeregningsresultatEntitet> orginaltResultat = beregningsresultatRepository.hentBgBeregningsresultat(originalBehandling.getId());
        Optional<BeregningsresultatEntitet> revurderingResultat = beregningsresultatRepository.hentBgBeregningsresultat(revurdering.getId());

        return ErEndringIBeregningsresultatFRISINN.finnEndringerIUtbetalinger(revurderingResultat, orginaltResultat, orginaltUttak)
            .stream()
            .anyMatch(endring -> endring.equals(UGUNST) || endring.equals(GUNST));
    }

    private boolean erNySøknadperiode(Behandling revurdering, Behandling origBehandling) {
        var nyttUttak = uttakRepository.hentFastsattUttak(revurdering.getId());
        var origUttak = uttakRepository.hentFastsattUttak(origBehandling.getId());

        return nyttUttak.getPerioder().size() > origUttak.getPerioder().size();
    }

}
