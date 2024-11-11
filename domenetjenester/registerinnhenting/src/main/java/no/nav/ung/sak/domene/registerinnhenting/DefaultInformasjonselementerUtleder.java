package no.nav.ung.sak.domene.registerinnhenting;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.ARBEIDSFORHOLD;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_BEREGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_PENSJONSGIVENDE;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_SAMMENLIGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.LIGNET_NÆRING;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.YTELSE;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class DefaultInformasjonselementerUtleder implements InformasjonselementerUtleder {

    private static final Map<BehandlingType, Set<RegisterdataType>> FILTER = Map.of(
        BehandlingType.FØRSTEGANGSSØKNAD,
        Set.of(
            YTELSE,
            ARBEIDSFORHOLD,
            INNTEKT_PENSJONSGIVENDE,
            LIGNET_NÆRING,
            INNTEKT_BEREGNINGSGRUNNLAG,
            INNTEKT_SAMMENLIGNINGSGRUNNLAG),
        BehandlingType.REVURDERING,
        Set.of(
            YTELSE,
            ARBEIDSFORHOLD,
            INNTEKT_PENSJONSGIVENDE,
            INNTEKT_BEREGNINGSGRUNNLAG,
            INNTEKT_SAMMENLIGNINGSGRUNNLAG));

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        return FILTER.get(behandlingType);
    }

}
