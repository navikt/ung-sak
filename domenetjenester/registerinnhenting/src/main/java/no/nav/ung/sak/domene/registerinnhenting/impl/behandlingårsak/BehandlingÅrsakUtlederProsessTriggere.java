package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

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
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object nyeste, Object eldste) {
        var eldsteGrunnlag = new HashSet<>(prosessTriggereRepository.hentGrunnlagBasertPåId((Long) eldste)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of));
        var nyesteGrunnlag = new HashSet<>(prosessTriggereRepository.hentGrunnlagBasertPåId((Long) nyeste)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of));

        nyesteGrunnlag.removeAll(eldsteGrunnlag);
        return nyesteGrunnlag.stream()
            .map(Trigger::getÅrsak)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
