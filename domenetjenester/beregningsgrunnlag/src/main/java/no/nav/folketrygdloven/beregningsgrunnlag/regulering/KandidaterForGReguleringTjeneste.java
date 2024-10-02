package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.kodeverk.GrunnbeløpReguleringStatus;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

@ApplicationScoped
public class KandidaterForGReguleringTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private KalkulusTjeneste kalkulusTjeneste;

    KandidaterForGReguleringTjeneste() {
    }

    @Inject
    public KandidaterForGReguleringTjeneste(BehandlingRepository behandlingRepository,
                                            VilkårResultatRepository vilkårResultatRepository,
                                            BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                            KalkulusTjeneste kalkulusTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    public List<DatoIntervallEntitet> skalGReguleres(Long fagsakId, DatoIntervallEntitet periode) {
        var sisteBehandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);

        if (sisteBehandlingOpt.isEmpty()) {
            return Collections.emptyList();
        }

        var sisteBehandling = sisteBehandlingOpt.get();

        if (sisteBehandling.erHenlagt()) {
            var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (behandling.isEmpty()) {
                return Collections.emptyList();
            }
            sisteBehandling = behandling.orElseThrow();
        }

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(sisteBehandling.getId());

        if (vilkårene.isEmpty()) {
            return Collections.emptyList();
        }

        var vilkår = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        if (vilkår.isEmpty()) {
            return Collections.emptyList();
        }

        var overlappendeVilkårsperioder = vilkår
            .orElseThrow(() -> new IllegalStateException("Fagsaken(id=" + fagsakId + ") har ikke beregnignsvilkåret knyttet til siste behandling"))
            .getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .filter(it -> periode.overlapper(it.getPeriode().getFomDato(), it.getFom()))
            .toList(); // FOM må være i perioden

        if (overlappendeVilkårsperioder.isEmpty()) {
            return Collections.emptyList();
        }

        var intervallerSomSkalGReguleres = finnPerioderForGregulering(sisteBehandling, overlappendeVilkårsperioder);

        return intervallerSomSkalGReguleres;
    }

    private List<DatoIntervallEntitet> finnPerioderForGregulering(Behandling sisteBehandling, List<VilkårPeriode> vilkårsperioder) {
        Saksnummer saksnummer = sisteBehandling.getFagsak().getSaksnummer();
        var bg = beregningPerioderGrunnlagRepository.hentGrunnlag(sisteBehandling.getId()).orElseThrow();
        var eksternReferanser = finnEksternreferanser(vilkårsperioder, bg);
        var eksternReferanserMedBehovForGRegulering = finnEksternReferanserForBgMedBehovForGRegulering(eksternReferanser, saksnummer);
        var stpMedBehovForGregulering = finnSkjæringstidspunkterFraEksternReferanse(eksternReferanserMedBehovForGRegulering, bg);
        var intervallerSomSkalGReguleres = finnVilkårsperioderFraSkjæringstidspunkt(vilkårsperioder, stpMedBehovForGregulering);
        return intervallerSomSkalGReguleres;
    }

    private static List<UUID> finnEksternreferanser(List<VilkårPeriode> vilkårsperioder, BeregningsgrunnlagPerioderGrunnlag bg) {
        return vilkårsperioder.stream()
            .flatMap(og -> bg.finnGrunnlagFor(og.getSkjæringstidspunkt()).stream())
            .map(BeregningsgrunnlagPeriode::getEksternReferanse)
            .toList();
    }

    private List<UUID> finnEksternReferanserForBgMedBehovForGRegulering(List<UUID> eksternReferanser, Saksnummer saksnummer) {
        Map<UUID, GrunnbeløpReguleringStatus> referanseMotVurderingsmap = kalkulusTjeneste.kontrollerBehovForGregulering(eksternReferanser, saksnummer);
        var eksternReferanserMedBehovForGRegulering = referanseMotVurderingsmap.entrySet()
            .stream()
            .filter(v -> v.getValue().equals(GrunnbeløpReguleringStatus.NØDVENDIG))
            .map(Map.Entry::getKey)
            .toList();
        return eksternReferanserMedBehovForGRegulering;
    }

    private static List<DatoIntervallEntitet> finnVilkårsperioderFraSkjæringstidspunkt(List<VilkårPeriode> vilkårsperioder, Set<LocalDate> skjæringstidspunkter) {
        return vilkårsperioder.stream().filter(g -> skjæringstidspunkter.contains(g.getSkjæringstidspunkt()))
            .map(VilkårPeriode::getPeriode)
            .toList();
    }

    private static Set<LocalDate> finnSkjæringstidspunkterFraEksternReferanse(List<UUID> koblingerMedBehovForGRegulering, BeregningsgrunnlagPerioderGrunnlag bg) {
        return koblingerMedBehovForGRegulering.stream()
            .flatMap(k -> bg.finnGrunnlagFor(k).stream())
            .map(p -> p.getSkjæringstidspunkt())
            .collect(Collectors.toSet());
    }

}
