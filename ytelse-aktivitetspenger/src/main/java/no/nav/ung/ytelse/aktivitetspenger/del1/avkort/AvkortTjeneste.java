package no.nav.ung.ytelse.aktivitetspenger.del1.avkort;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.time.LocalDate;
import java.util.Comparator;

@Dependent
public class AvkortTjeneste {

    private final StartdatoRepository startdatoRepository;

    @Inject
    public AvkortTjeneste(StartdatoRepository startdatoRepository) {
        this.startdatoRepository = startdatoRepository;
    }

    public LocalDateTimeline<Boolean> tidslinjeForMuligAvkorting(Long behandlingId) {
        StartdatoGrunnlag startdatoGrunnlagOpt = startdatoRepository.hentGrunnlag(behandlingId).orElse(null);
        return utledTidslinjeForMuligAvkorting(startdatoGrunnlagOpt);
    }

    static LocalDateTimeline<Boolean> utledTidslinjeForMuligAvkorting(StartdatoGrunnlag startdatoGrunnlag) {
        if (startdatoGrunnlag == null) {
            return LocalDateTimeline.empty();
        }
        LocalDate sistSøkteStartdatoIBehandlingen = startdatoGrunnlag.getRelevanteStartdatoer().getStartdatoer()
            .stream()
            .map(SøktStartdato::getStartdato)
            .max(Comparator.naturalOrder())
            .orElse(null);
        if (sistSøkteStartdatoIBehandlingen == null) {
            return LocalDateTimeline.empty();
        }
        return AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraSøktDato(sistSøkteStartdatoIBehandlingen)
            .disjoint(new LocalDateTimeline<>(sistSøkteStartdatoIBehandlingen, sistSøkteStartdatoIBehandlingen, true)); //fjerner startdatoen siden saksbehandler må ta stilling til vilkår på denne datoen
    }
}
