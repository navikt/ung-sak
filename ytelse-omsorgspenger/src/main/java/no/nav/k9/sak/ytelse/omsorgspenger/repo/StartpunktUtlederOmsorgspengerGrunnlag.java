package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;

@ApplicationScoped
@GrunnlagRef("OmsorgspengerGrunnlag")
@FagsakYtelseTypeRef("OMP")
public class StartpunktUtlederOmsorgspengerGrunnlag implements EndringStartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();

    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    public StartpunktUtlederOmsorgspengerGrunnlag() {
        // For CDI
    }

    @Inject
    StartpunktUtlederOmsorgspengerGrunnlag(OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyGrunnlagId, Object origGrunnlagId) {
        return hentStartpunktFor((Long) nyGrunnlagId, (Long) origGrunnlagId)
            .orElse(StartpunktType.UDEFINERT);
    }

    private Optional<StartpunktType> hentStartpunktFor(Long nyGrunnlagId, Long origGrunnlagId) {

        var orginaleJournalposter = grunnlagRepository.hentGrunnlagBasertPåId(origGrunnlagId).stream()
            .map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad)
            .filter(Objects::nonNull)
            .map(OppgittFravær::getPerioder)
            .flatMap(Collection::stream)
            .map(OppgittFraværPeriode::getJournalpostId)
            .collect(Collectors.toSet());

        var nyeJournalposter = grunnlagRepository.hentGrunnlagBasertPåId(nyGrunnlagId).stream()
            .map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad)
            .filter(Objects::nonNull)
            .map(OppgittFravær::getPerioder)
            .flatMap(Collection::stream)
            .map(OppgittFraværPeriode::getJournalpostId)
            .collect(Collectors.toSet());
        nyeJournalposter.removeAll(orginaleJournalposter);

        if (!nyeJournalposter.isEmpty()) {
            loggStartpunkt(StartpunktType.INIT_PERIODER, nyGrunnlagId, origGrunnlagId, "Tilkommet ny søknad(er) '" + nyeJournalposter + "'.");
            return Optional.of(StartpunktType.INIT_PERIODER);
        }

        return Optional.empty();
    }

    private void loggStartpunkt(StartpunktType startpunkt, Long nyGrunnlagId, Long origGrunnlagId, String endringLoggtekst) {
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, nyGrunnlagId, origGrunnlagId);
    }

}
