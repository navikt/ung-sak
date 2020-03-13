package no.nav.foreldrepenger.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsyn;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæringer;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.medisinsk.Legeerklæring;
import no.nav.k9.sak.kontrakt.medisinsk.PeriodeMedTilsyn;
import no.nav.k9.sak.kontrakt.medisinsk.PeriodeMedTilsynOgÅrsakssammenheng;
import no.nav.k9.sak.kontrakt.medisinsk.SykdomsDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
class SykdomDtoMapper {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private UttakRepository uttakRepository;

    SykdomDtoMapper() {
        // CDI
    }

    @Inject
    public SykdomDtoMapper(MedisinskGrunnlagRepository medisinskGrunnlagRepository, UttakRepository uttakRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.uttakRepository = uttakRepository;
    }

    SykdomsDto map(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isPresent()) {
            var periode = mapTilPeriode(søknadsperioder.get());
            final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
            if (medisinskGrunnlag.isPresent()) {
                final var grunnlag = medisinskGrunnlag.get();
                final var legeerklæringer = grunnlag.getLegeerklæringer();
                final var kontinuerligTilsyn = grunnlag.getKontinuerligTilsyn();

                return new SykdomsDto(periode,
                    mapLegeerklæringer(legeerklæringer),
                    mapPerioderMedKontinuerligTilsyn(kontinuerligTilsyn),
                    mapPerioderMedUtvidetTilsyn(kontinuerligTilsyn));
            }
            return new SykdomsDto(periode);
        }
        return null;
    }

    private Periode mapTilPeriode(Søknadsperioder fordeling) {
        final var perioder = fordeling.getPerioder();
        final var fom = perioder.stream()
            .map(Søknadsperiode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        final var tom = perioder.stream()
            .map(Søknadsperiode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();
        return new Periode(fom, tom);
    }

    private List<PeriodeMedTilsyn> mapPerioderMedUtvidetTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        if (kontinuerligTilsyn == null) {
            return List.of();
        }
        return kontinuerligTilsyn.getPerioder()
            .stream()
            .filter(periode -> periode.getGrad() > 100)
            .map(p -> new PeriodeMedTilsyn(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getBegrunnelse()))
            .collect(Collectors.toList());
    }

    private List<PeriodeMedTilsynOgÅrsakssammenheng> mapPerioderMedKontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        if (kontinuerligTilsyn == null) {
            return List.of();
        }
        return kontinuerligTilsyn.getPerioder()
            .stream()
            .filter(periode -> periode.getGrad() == 100)
            .map(p -> new PeriodeMedTilsynOgÅrsakssammenheng(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getBegrunnelse(), p.getÅrsaksammenheng(), p.getÅrsaksammenhengBegrunnelse()))
            .collect(Collectors.toList());
    }

    private List<Legeerklæring> mapLegeerklæringer(Legeerklæringer legeerklæringer) {
        if (legeerklæringer == null) {
            return List.of();
        }
        return legeerklæringer.getLegeerklæringer()
            .stream()
            .map(this::mapTilLegeerklæring)
            .collect(Collectors.toList());
    }

    private Legeerklæring mapTilLegeerklæring(no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæring it) {
        return new Legeerklæring(it.getDatert(),
            it.getDatert(),
            it.getUuid(),
            it.getKilde().getKode(),
            it.getDiagnose(),
            it.getInnleggelsesPerioder()
                .stream()
                .map(ip -> new Periode(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()))
                .collect(Collectors.toList()));
    }
}
