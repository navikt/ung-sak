package no.nav.foreldrepenger.web.app.tjenester.behandling.sykdom;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsyn;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæringer;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.kontrakt.medisinsk.Legeerklæring;
import no.nav.k9.sak.kontrakt.medisinsk.PeriodeMedTilsyn;
import no.nav.k9.sak.kontrakt.medisinsk.SykdomsDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
class SykdomDtoMapper {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    SykdomDtoMapper() {
        // CDI
    }

    @Inject
    public SykdomDtoMapper(MedisinskGrunnlagRepository medisinskGrunnlagRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
    }

    SykdomsDto map(Long behandlingId) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (medisinskGrunnlag.isPresent()) {
            final var grunnlag = medisinskGrunnlag.get();
            final var legeerklæringer = grunnlag.getLegeerklæringer();
            final var kontinuerligTilsyn = grunnlag.getKontinuerligTilsyn();

            return new SykdomsDto(mapLegeerklæringer(legeerklæringer),
                mapPerioderMedKontinuerligTilsyn(kontinuerligTilsyn),
                mapPerioderMedUtvidetTilsyn(kontinuerligTilsyn));
        }
        return null;
    }

    private List<PeriodeMedTilsyn> mapPerioderMedUtvidetTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        return kontinuerligTilsyn.getPerioder()
            .stream()
            .filter(periode -> periode.getGrad() > 100)
            .map(p -> new PeriodeMedTilsyn(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getBegrunnelse()))
            .collect(Collectors.toList());
    }

    private List<PeriodeMedTilsyn> mapPerioderMedKontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        return kontinuerligTilsyn.getPerioder()
            .stream()
            .filter(periode -> periode.getGrad() == 100)
            .map(p -> new PeriodeMedTilsyn(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getBegrunnelse()))
            .collect(Collectors.toList());
    }

    private List<Legeerklæring> mapLegeerklæringer(Legeerklæringer legeerklæringer) {
        return legeerklæringer.getLegeerklæringer()
            .stream()
            .map(this::mapTilLegeerklæring)
            .collect(Collectors.toList());
    }

    private Legeerklæring mapTilLegeerklæring(no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæring it) {
        return new Legeerklæring(it.getPeriode().getFomDato(),
            it.getPeriode().getTomDato(),
            it.getUuid(),
            it.getKilde().getKode(),
            it.getDiagnose(),
            it.getInnleggelsesPerioder()
                .stream()
                .map(ip -> new Periode(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()))
                .collect(Collectors.toList()));
    }
}
