package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.registerinnhenting.InformasjonselementerUtleder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_UNGDOMSYTELSEGRUNNLAG;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngdomsytelseInformasjonselementerUtleder implements InformasjonselementerUtleder {

    private static final Map<BehandlingType, Set<RegisterdataType>> FILTER = Map.of(
        BehandlingType.FØRSTEGANGSSØKNAD,
        Set.of(
            INNTEKT_UNGDOMSYTELSEGRUNNLAG),
        BehandlingType.REVURDERING,
        Set.of(
            INNTEKT_UNGDOMSYTELSEGRUNNLAG));

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        var informasjonselementer = FILTER.get(behandlingType);
        if (informasjonselementer == null) {
            throw new IllegalArgumentException("Har ikke konfigurert informasjonselementer for ungdomsytelse og behandlingType=" + behandlingType);
        }
        return informasjonselementer;
    }

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType, Collection<BehandlingÅrsakType> behandlingÅrsaker) {
        return erKontrollAvInntekt(behandlingÅrsaker) ? utled(behandlingType) : Set.of();
    }

    private static boolean erKontrollAvInntekt(Collection<BehandlingÅrsakType> behandlingÅrsaker) {
        return BehandlingÅrsakType.årsakerForInnhentingAvInntektOgYtelse().stream().anyMatch(behandlingÅrsaker::contains);
    }
}
