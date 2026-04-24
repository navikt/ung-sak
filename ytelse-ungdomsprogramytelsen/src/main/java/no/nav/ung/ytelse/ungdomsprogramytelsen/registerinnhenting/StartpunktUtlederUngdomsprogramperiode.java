package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@GrunnlagRef(UngdomsprogramPeriodeGrunnlag.class)
@FagsakYtelseTypeRef
class StartpunktUtlederUngdomsprogramperiode implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederUngdomsprogramperiode.class);
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    StartpunktUtlederUngdomsprogramperiode() {
        // For CDI
    }

    @Inject
    public StartpunktUtlederUngdomsprogramperiode(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object nyeste, Object eldste) {
        var eldsteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) eldste);
        var nyesteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) nyeste);

        var eldstePerioder = hentPerioder(eldsteGrunnlag);
        var nyestePerioder = hentPerioder(nyesteGrunnlag);

        boolean harEndretUtvidetKvote = !eldsteGrunnlag.map(UngdomsprogramPeriodeGrunnlag::isHarUtvidetKvote).orElse(false)
            .equals(nyesteGrunnlag.map(UngdomsprogramPeriodeGrunnlag::isHarUtvidetKvote).orElse(false));

        if (nyestePerioder.equals(eldstePerioder) && !harEndretUtvidetKvote) {
            return StartpunktType.UDEFINERT;
        }

        log.info("Fant endringer i ungdomsprogramperioder{}. Flytter til init perioder.",
            harEndretUtvidetKvote ? " (utvidet kvote endret)" : "");

        return StartpunktType.INIT_PERIODER;
    }

    private Set<DatoIntervallEntitet> hentPerioder(Optional<UngdomsprogramPeriodeGrunnlag> grunnlag) {
        return grunnlag
            .stream()
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Set::stream)
            .map(UngdomsprogramPeriode::getPeriode)
            .collect(Collectors.toCollection(HashSet::new));
    }


}
