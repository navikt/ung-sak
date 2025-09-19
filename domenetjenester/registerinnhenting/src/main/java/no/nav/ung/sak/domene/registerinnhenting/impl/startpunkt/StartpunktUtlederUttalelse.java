package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseGrunnlag;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.behandlingslager.uttalelse.Uttalelser;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@GrunnlagRef(UttalelseGrunnlag.class)
@FagsakYtelseTypeRef
class StartpunktUtlederUttalelse implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederUttalelse.class);
    private UttalelseRepository uttalelseRepository;

    @Inject
    public StartpunktUtlederUttalelse(UttalelseRepository uttalelseRepository) {
        this.uttalelseRepository = uttalelseRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyeste, Object eldste) {
        var eldsteUttalelser = hentUttalelser((Long) eldste);
        var nyesteUttalelser = hentUttalelser((Long) nyeste);
        if (nyesteUttalelser.equals(eldsteUttalelser)) {
            return StartpunktType.UDEFINERT;
        }
        log.info("Fant endringer i uttalelser. Flytter til init perioder.");
        return StartpunktType.INIT_PERIODER;
    }

    private Set<JournalpostId> hentUttalelser(Long nyeste) {
        return uttalelseRepository.hentGrunnlagBasertPåId(nyeste)
            .stream()
            .map(UttalelseGrunnlag::getUttalelser)
            .map(Uttalelser::getUttalelser)
            .flatMap(Set::stream)
            .map(UttalelseV2::getSvarJournalpostId)
            .collect(Collectors.toCollection(HashSet::new));
    }


}
