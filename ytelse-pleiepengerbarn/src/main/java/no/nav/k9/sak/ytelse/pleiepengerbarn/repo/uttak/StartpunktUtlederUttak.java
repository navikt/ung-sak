package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;

@ApplicationScoped
@GrunnlagRef(UttaksPerioderGrunnlag.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class StartpunktUtlederUttak implements EndringStartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();

    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    public StartpunktUtlederUttak() {
        // For CDI
    }

    @Inject
    StartpunktUtlederUttak(UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyttGrunnlag, Object gammeltGrunnlag) {
        return hentAlleStartpunktFor(ref, (Long) nyttGrunnlag, (Long) gammeltGrunnlag)
            .stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktFor(BehandlingReferanse ref, Long nyttGrunnlag, Long gammeltGrunnlag) { // NOSONAR

        var startpunkter = new ArrayList<StartpunktType>();

        var orginaleJournalposter = uttakPerioderGrunnlagRepository.hentGrunnlagBasertPåId(gammeltGrunnlag)
            .stream()
            .map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .flatMap(Collection::stream)
            .map(PerioderFraSøknad::getJournalpostId)
            .collect(Collectors.toSet());

        var nyeJournalposter = uttakPerioderGrunnlagRepository.hentGrunnlagBasertPåId(nyttGrunnlag)
            .stream()
            .map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .flatMap(Collection::stream)
            .map(PerioderFraSøknad::getJournalpostId)
            .collect(Collectors.toSet());

        nyeJournalposter.removeAll(orginaleJournalposter);

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
