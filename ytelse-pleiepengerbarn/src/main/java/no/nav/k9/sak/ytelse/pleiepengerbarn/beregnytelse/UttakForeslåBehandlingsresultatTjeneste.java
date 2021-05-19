package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class UttakForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private UttakRepository uttakRepository;

    UttakForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public UttakForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                   VedtakVarselRepository vedtakVarselRepository,
                                                   UttakRepository uttakRepository,
                                                   @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.uttakRepository = uttakRepository;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        var f = uttakRepository.hentOppgittUttak(behandlingId);
        var maksPeriode = f.getMaksPeriode();
        return maksPeriode;
    }

    @Override
    protected boolean skalBehandlingenSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        if (skalAvslåsBasertPåAndreForhold(ref)) {
            return true;
        }
        
        final var maksPeriode = getMaksPeriode(ref.getBehandlingId());
        final var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(maksPeriode);
        
        final var avslåtteVilkår = vilkårTidslinjer.entrySet().stream()
            .filter(e -> harAvslåtteVilkårsPerioder(e.getValue())
                    && harIngenOppfylteVilkårsPerioder(e.getValue())
            )
            .map(e -> e.getKey())
            .collect(Collectors.toList());
        
        if (avslåtteVilkår.isEmpty()) {
            return false;
        }
        
        if (avslåtteVilkår.stream().anyMatch(v -> v != VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR && v != VilkårType.MEDISINSKEVILKÅR_18_ÅR)) {
            return true;
        }
        
        final var ingenAvSykdomsvilkåreneErOppfylt = harIngenOppfylteVilkårsPerioder(vilkårTidslinjer.get(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR))
                && harIngenOppfylteVilkårsPerioder(vilkårTidslinjer.get(VilkårType.MEDISINSKEVILKÅR_18_ÅR));
        
        
        return ingenAvSykdomsvilkåreneErOppfylt;
    }
}
