package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;

@ApplicationScoped
@GrunnlagRef("ProsessTriggere")
@FagsakYtelseTypeRef("*")
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
        var grunnlag1 = prosessTriggereRepository.hentGrunnlagBasertPåId((Long) grunnlagId1)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of);
        var grunnlag2 = prosessTriggereRepository.hentGrunnlagBasertPåId((Long) grunnlagId2)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of);

        return grunnlag2.stream()
            .filter(it -> grunnlag1.stream()
                .noneMatch(at -> Objects.equals(at.getÅrsak(), it.getÅrsak())
                    && Objects.equals(at.getPeriode(), it.getPeriode())))
            .map(Trigger::getÅrsak)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
