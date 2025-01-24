package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@GrunnlagRef(UngdomsytelseStartdatoGrunnlag.class)
@FagsakYtelseTypeRef
class StartpunktUtlederStartdatoer implements EndringStartpunktUtleder {
    private String klassenavn = this.getClass().getSimpleName();

    private UngdomsytelseStartdatoRepository startdatoRepository;

    public StartpunktUtlederStartdatoer() {
        // For CDI
    }

    @Inject
    StartpunktUtlederStartdatoer(UngdomsytelseStartdatoRepository uttakPerioderGrunnlagRepository) {
        this.startdatoRepository = uttakPerioderGrunnlagRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyttGrunnlag, Object gammeltGrunnlag) {
        return hentAlleStartpunktFor((Long) nyttGrunnlag, (Long) gammeltGrunnlag)
            .stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktFor(Long nyttGrunnlag, Long gammeltGrunnlag) { // NOSONAR

        var startpunkter = new ArrayList<StartpunktType>();

        // Finner alle originale journalposter
        var orginaleJournalposter = startdatoRepository.hentGrunnlagBasertPåId(gammeltGrunnlag)
            .stream()
            .map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer)
            .map(UngdomsytelseStartdatoer::getStartdatoer)
            .flatMap(Collection::stream)
            .map(UngdomsytelseSøktStartdato::getJournalpostId)
            .collect(Collectors.toSet());

        // Finner alle nye journalposter
        var nyeJournalposter = startdatoRepository.hentGrunnlagBasertPåId(nyttGrunnlag)
            .stream()
            .map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer)
            .map(UngdomsytelseStartdatoer::getStartdatoer)
            .flatMap(Collection::stream)
            .map(UngdomsytelseSøktStartdato::getJournalpostId)
            .collect(Collectors.toSet());

        nyeJournalposter.removeAll(orginaleJournalposter);

        // Sjekker om vi har noen nye som ikke var i originalt grunnlag
        if (!nyeJournalposter.isEmpty()) {
            leggTilStartpunkt(startpunkter, nyttGrunnlag, gammeltGrunnlag, StartpunktType.INIT_PERIODER, "Tilkommet ny søknad(er) '" + nyeJournalposter + "'.");
        }

        return startpunkter;
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, Long grunnlagId1, Long grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
