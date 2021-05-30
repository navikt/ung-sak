package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.util.Comparator;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;

@ApplicationScoped
@GrunnlagRef("ProsessTriggere")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("OMP")
class StartpunktUtlederProsessTriggere implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederProsessTriggere.class);

    private ProsessTriggereRepository prosessTriggereRepository;

    StartpunktUtlederProsessTriggere() {
        // For CDI
    }

    @Inject
    public StartpunktUtlederProsessTriggere(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = prosessTriggereRepository.hentGrunnlagBasertPåId((Long) grunnlagId1)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of);
        var grunnlag2 = prosessTriggereRepository.hentGrunnlagBasertPåId((Long) grunnlagId2)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of);

        grunnlag2.removeAll(grunnlag1);

        return grunnlag2.stream().map(this::mapTilStartPunktType)
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType mapTilStartPunktType(Trigger it) {
        if (BehandlingÅrsakType.RE_SATS_REGULERING.equals(it.getÅrsak())) {
            return StartpunktType.BEREGNING;
        }
        log.info("Ukjent trigger {} med ukjent startpunkt, starter fra starten", it.getÅrsak());
        return StartpunktType.INIT_PERIODER;
    }


}
