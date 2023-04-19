package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;

@ApplicationScoped
public class KandidaterForInntektgraderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private KalkulusTjeneste kalkulusTjeneste;

    KandidaterForInntektgraderingTjeneste() {
    }

    @Inject
    public KandidaterForInntektgraderingTjeneste(BehandlingRepository behandlingRepository,
                                                 VilkårResultatRepository vilkårResultatRepository,
                                                 BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                 KalkulusTjeneste kalkulusTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    public Set<DatoIntervallEntitet> finnGraderingMotInntektPerioder(Long fagsakId, LocalDate dato) {
        var sisteBehandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);

        if (sisteBehandlingOpt.isEmpty()) {
            return Collections.emptySet();
        }

        var sisteBehandling = sisteBehandlingOpt.get();

        if (sisteBehandling.erHenlagt()) {
            var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (behandling.isEmpty()) {
                return Collections.emptySet();
            }
            sisteBehandling = behandling.orElseThrow();
        }

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(sisteBehandling.getId());

        if (vilkårene.isEmpty()) {
            return Collections.emptySet();
        }

        var vilkår = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        if (vilkår.isEmpty()) {
            return Collections.emptySet();
        }

        var overlappendeGrunnlag = vilkår
            .orElseThrow(() -> new IllegalStateException("Fagsaken(id=" + fagsakId + ") har ikke beregningsvilkåret knyttet til siste behandling"))
            .getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .filter(it -> it.getPeriode().overlapper(dato, dato))
            .toList();

        if (overlappendeGrunnlag.isEmpty()) {
            return Collections.emptySet();
        }

        Saksnummer saksnummer = sisteBehandling.getFagsak().getSaksnummer();
        var bg = beregningPerioderGrunnlagRepository.hentGrunnlag(sisteBehandling.getId()).orElseThrow();

        Map<UUID, DatoIntervallEntitet> koblingerÅSpørreMot = new HashMap<>();

        overlappendeGrunnlag.forEach(og ->
            bg.finnGrunnlagFor(og.getSkjæringstidspunkt()).ifPresent(bgp ->
                koblingerÅSpørreMot.put(bgp.getEksternReferanse(), finnPeriode(dato, og))));

        Map<UUID, List<DatoIntervallEntitet>> koblingMotVurderingsmap = kalkulusTjeneste.simulerTilkommetInntekt(koblingerÅSpørreMot, saksnummer);


        return koblingMotVurderingsmap.values().stream().flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static DatoIntervallEntitet finnPeriode(LocalDate dato, VilkårPeriode og) {
        return dato.isAfter(og.getSkjæringstidspunkt()) ?
            DatoIntervallEntitet.fraOgMedTilOgMed(dato, og.getTom())
            : og.getPeriode();
    }

}
