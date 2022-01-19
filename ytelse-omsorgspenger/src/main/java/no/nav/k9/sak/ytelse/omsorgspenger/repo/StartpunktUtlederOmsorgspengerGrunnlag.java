package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;
import no.nav.k9.sak.typer.JournalpostId;

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
        var grunnlagOrig = grunnlagRepository.hentGrunnlagBasertPåId(origGrunnlagId);
        Set<JournalpostId> orginaleJournalposter = grunnlagOrig.map(grunnlag -> hentJournalposter(grunnlag)).orElse(Set.of());

        var grunnlagNytt = grunnlagRepository.hentGrunnlagBasertPåId(nyGrunnlagId);
        Set<JournalpostId> nyeJournalposter = grunnlagNytt.map(grunnlag -> hentJournalposter(grunnlag)).orElse(Set.of());
        nyeJournalposter.removeAll(orginaleJournalposter);

        if (!nyeJournalposter.isEmpty()) {
            loggStartpunkt(StartpunktType.INIT_PERIODER, nyGrunnlagId, origGrunnlagId, "Tilkommet ny søknad(er) '" + nyeJournalposter + "'.");
            return Optional.of(StartpunktType.INIT_PERIODER);
        }

        return Optional.empty();
    }

    private Set<JournalpostId> hentJournalposter(OmsorgspengerGrunnlag grunnlag) {
        Set<JournalpostId> journalposter = new HashSet<>();
        var fraSøknad = grunnlag.getOppgittFraværFraSøknad();
        if (fraSøknad != null) {
            journalposter.addAll(fraSøknad.getPerioder().stream().map(OppgittFraværPeriode::getJournalpostId).collect(Collectors.toSet()));
        }
        var fraKorrigeringIm = grunnlag.getOppgittFraværFraKorrigeringIm();
        if (fraKorrigeringIm != null) {
            journalposter.addAll(fraKorrigeringIm.getPerioder().stream().map(OppgittFraværPeriode::getJournalpostId).collect(Collectors.toSet()));
        }
        return journalposter;
    }

    private void loggStartpunkt(StartpunktType startpunkt, Long nyGrunnlagId, Long origGrunnlagId, String endringLoggtekst) {
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, nyGrunnlagId, origGrunnlagId);
    }

}
