package no.nav.ung.ytelse.aktivitetspenger.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
import java.util.stream.Collectors;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.*;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef
public class AktivitetspengerInformasjonselementerUtleder implements InformasjonselementerUtleder {

    private static final Map<BehandlingType, Set<RegisterdataType>> FILTER = Map.of(
        BehandlingType.FØRSTEGANGSSØKNAD,
        Set.of(
            INNTEKT_AKTIVITETSPENGERGRUNNLAG,
            LIGNET_NÆRING),
        BehandlingType.REVURDERING,
        Set.of(
            INNTEKT_AKTIVITETSPENGERGRUNNLAG,
            LIGNET_NÆRING));

    private boolean aktivitetspengerInntektsgrunnlagEnabled;

    AktivitetspengerInformasjonselementerUtleder() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerInformasjonselementerUtleder(@KonfigVerdi(value = "AKTIVITETSPENGER_INNTEKTSGRUNNLAG_ENABLED", required = false, defaultVerdi = "false") boolean aktivitetspengerInntektsgrunnlagEnabled) {
        this.aktivitetspengerInntektsgrunnlagEnabled = aktivitetspengerInntektsgrunnlagEnabled;
    }

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        var informasjonselementer = FILTER.get(behandlingType);
        if (informasjonselementer == null) {
            throw new IllegalArgumentException("Har ikke konfigurert informasjonselementer for aktivitetspenger og behandlingType=" + behandlingType);
        }
        if (aktivitetspengerInntektsgrunnlagEnabled) {
            return informasjonselementer;
        }
        return informasjonselementer.stream()
            .map(it -> it == INNTEKT_AKTIVITETSPENGERGRUNNLAG ? INNTEKT_UNGDOMSYTELSEGRUNNLAG : it)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType, Collection<BehandlingÅrsakType> behandlingÅrsaker) {
        if (!aktivitetspengerInntektsgrunnlagEnabled || erKontrollAvInntekt(behandlingÅrsaker)) {
            return utled(behandlingType);
        }
        return utled(behandlingType).stream()
            .filter(it -> it != INNTEKT_AKTIVITETSPENGERGRUNNLAG)
            .collect(Collectors.toSet());
    }

    private static boolean erKontrollAvInntekt(Collection<BehandlingÅrsakType> behandlingÅrsaker) {
        return BehandlingÅrsakType.årsakerForInnhentingAvInntektOgYtelse().stream().anyMatch(behandlingÅrsaker::contains);
    }
}
