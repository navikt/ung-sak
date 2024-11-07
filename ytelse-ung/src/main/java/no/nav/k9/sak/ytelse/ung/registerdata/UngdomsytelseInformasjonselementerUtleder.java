package no.nav.k9.sak.ytelse.ung.registerdata;

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
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngdomsytelseInformasjonselementerUtleder implements InformasjonselementerUtleder {

    private static final Map<BehandlingType, Set<RegisterdataType>> FILTER = Map.of(
        BehandlingType.FØRSTEGANGSSØKNAD,
        Set.of(
            ARBEIDSFORHOLD,
            INNTEKT_BEREGNINGSGRUNNLAG),
        BehandlingType.REVURDERING,
        Set.of(
            ARBEIDSFORHOLD,
            INNTEKT_BEREGNINGSGRUNNLAG));

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        return FILTER.get(behandlingType);
    }

}
