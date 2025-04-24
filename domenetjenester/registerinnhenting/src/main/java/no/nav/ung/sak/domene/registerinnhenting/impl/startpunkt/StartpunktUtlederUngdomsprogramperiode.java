package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

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
        var eldsteGrunnlag = new HashSet<>(ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) eldste)
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .orElseGet(Set::of));
        var nyesteGrunnlag = new HashSet<>(ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) nyeste)
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .orElseGet(Set::of));

        nyesteGrunnlag.removeAll(eldsteGrunnlag);

        if (nyesteGrunnlag.isEmpty()) {
            return StartpunktType.UDEFINERT;
        }

        log.info("Fant endringer i ungdomsprogramperioder. Flytter til innhenting av registeropplysninger.");

        return StartpunktType.INNHENT_REGISTEROPPLYSNINGER;
    }


}
