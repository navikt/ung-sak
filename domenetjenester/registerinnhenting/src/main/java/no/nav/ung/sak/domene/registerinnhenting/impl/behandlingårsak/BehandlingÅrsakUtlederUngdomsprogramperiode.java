package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@GrunnlagRef(UngdomsprogramPeriodeGrunnlag.class)
@FagsakYtelseTypeRef
public class BehandlingÅrsakUtlederUngdomsprogramperiode implements BehandlingÅrsakUtleder {

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;


    BehandlingÅrsakUtlederUngdomsprogramperiode() {
    }

    @Inject
    public BehandlingÅrsakUtlederUngdomsprogramperiode(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object nyeste, Object eldste) {
        var eldsteGrunnlag = new HashSet<>(ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) eldste)
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .orElseGet(Set::of));
        var nyesteGrunnlag = new HashSet<>(ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) nyeste)
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .orElseGet(Set::of));

        final var årsaker = new HashSet<BehandlingÅrsakType>();

        var gamleFomDatoer = eldsteGrunnlag.stream().map(it -> it.getPeriode().getFomDato()).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        var nyeFomDatoer = nyesteGrunnlag.stream().map(it -> it.getPeriode().getFomDato()).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        if (nyeFomDatoer.equals(gamleFomDatoer)) {
            årsaker.add(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        }

        var gamleTomDatoer = eldsteGrunnlag.stream().map(it -> it.getPeriode().getTomDato()).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        var nyeTomDatoer = nyesteGrunnlag.stream().map(it -> it.getPeriode().getTomDato()).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        if (nyeTomDatoer.equals(gamleTomDatoer)) {
            årsaker.add(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        }
        return årsaker;
    }
}
