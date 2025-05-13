package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.registerinnhenting.InformasjonselementerUtleder;

import java.util.Map;
import java.util.Set;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_SAMMENLIGNINGSGRUNNLAG;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngdomsytelseInformasjonselementerUtleder implements InformasjonselementerUtleder {

    private static final Map<BehandlingType, Set<RegisterdataType>> FILTER = Map.of(
        BehandlingType.FØRSTEGANGSSØKNAD,
        Set.of(
            INNTEKT_SAMMENLIGNINGSGRUNNLAG),
        BehandlingType.REVURDERING,
        Set.of(
            INNTEKT_SAMMENLIGNINGSGRUNNLAG),
        BehandlingType.KONTROLLBEHANDLING,
        Set.of(
            INNTEKT_SAMMENLIGNINGSGRUNNLAG));

    @Override
    public Set<RegisterdataType> utled(BehandlingType behandlingType) {
        return FILTER.get(behandlingType);
    }

}
