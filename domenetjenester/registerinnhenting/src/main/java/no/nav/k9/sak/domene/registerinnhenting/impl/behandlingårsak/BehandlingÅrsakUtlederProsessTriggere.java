package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;

@ApplicationScoped
@GrunnlagRef(ProsessTriggere.class)
@FagsakYtelseTypeRef
public class BehandlingÅrsakUtlederProsessTriggere implements BehandlingÅrsakUtleder {

    private ProsessTriggereRepository prosessTriggereRepository;

    BehandlingÅrsakUtlederProsessTriggere() {
    }

    @Inject
    public BehandlingÅrsakUtlederProsessTriggere(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag2 = prosessTriggereRepository.hentGrunnlagBasertPåId((Long) grunnlagId2)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of);

        return grunnlag2.stream()
            .map(Trigger::getÅrsak)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
