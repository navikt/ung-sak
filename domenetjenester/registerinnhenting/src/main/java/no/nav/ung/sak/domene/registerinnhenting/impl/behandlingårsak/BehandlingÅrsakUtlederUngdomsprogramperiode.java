package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Collection;
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
        var eldstePerioder = ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) eldste)
            .stream()
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Collection::stream)
            .map(UngdomsprogramPeriode::getPeriode)
            .sorted()
            .collect(Collectors.toCollection(LinkedHashSet::new));
        var nyestePerioder = ungdomsprogramPeriodeRepository.hentGrunnlagBasertPåId((Long) nyeste)
            .stream()
            .map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Collection::stream)
            .map(UngdomsprogramPeriode::getPeriode)
            .sorted()
            .collect(Collectors.toCollection(LinkedHashSet::new));

        final var årsaker = new HashSet<BehandlingÅrsakType>();

        var gamleFomDatoer = eldstePerioder.stream().map(DatoIntervallEntitet::getFomDato).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        var nyeFomDatoer = nyestePerioder.stream().map(DatoIntervallEntitet::getFomDato).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        if (!nyeFomDatoer.equals(gamleFomDatoer)) {
            årsaker.add(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        }

        var gamleTomDatoer = eldstePerioder.stream().map(DatoIntervallEntitet::getTomDato).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        var nyeTomDatoer = nyestePerioder.stream().map(DatoIntervallEntitet::getTomDato).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
        if (!nyeTomDatoer.equals(gamleTomDatoer)) {
            årsaker.add(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        }
        return årsaker;
    }
}
