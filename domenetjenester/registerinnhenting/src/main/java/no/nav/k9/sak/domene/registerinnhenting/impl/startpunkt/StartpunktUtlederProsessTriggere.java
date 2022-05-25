package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
@GrunnlagRef(ProsessTriggere.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OMSORGSPENGER)
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
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyeste, Object eldste) {
        var eldsteGrunnlag = new HashSet<>(prosessTriggereRepository.hentGrunnlagBasertPåId((Long) eldste)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of));
        var nyesteGrunnlag = new HashSet<>(prosessTriggereRepository.hentGrunnlagBasertPåId((Long) nyeste)
            .map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of));

        nyesteGrunnlag.removeAll(eldsteGrunnlag);

        return nyesteGrunnlag.stream()
            .map(this::mapTilStartPunktType)
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType mapTilStartPunktType(Trigger it) {
        if (BehandlingÅrsakType.RE_SATS_REGULERING.equals(it.getÅrsak())) {
            return StartpunktType.BEREGNING;
        }
        if (Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON, BehandlingÅrsakType.RE_UTSATT_BEHANDLING).contains(it.getÅrsak())) {
            return StartpunktType.UTTAKSVILKÅR;
        }
        log.info("Ukjent trigger {} med ukjent startpunkt, starter fra starten", it.getÅrsak());
        return StartpunktType.INIT_PERIODER;
    }


}
