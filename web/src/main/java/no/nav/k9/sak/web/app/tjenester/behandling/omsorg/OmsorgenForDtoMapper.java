package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.OmsorgenFor;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.medisinsk.OmsorgenForDto;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
class OmsorgenForDtoMapper {

    private UttakRepository uttakRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    OmsorgenForDtoMapper() {
        // CDI
    }

    @Inject
    public OmsorgenForDtoMapper(UttakRepository uttakRepository, MedisinskGrunnlagRepository medisinskGrunnlagRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.uttakRepository = uttakRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public OmsorgenForDto map(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isPresent()) {
            var periode = mapTilPeriode(søknadsperioder.get());
            var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
            var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
            if (pleietrengende.isPresent()) {
                var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
                if (optAggregat.isPresent()) {
                    var aggregat = optAggregat.get();
                    var pleietrengendeAktørId = pleietrengende.get();

                    var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

                    var morEllerFar = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
                    var sammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);
                    var harOmsorgenFor = utledOmsorgenFor(medisinskGrunnlag, morEllerFar, sammeBosted);

                    return new OmsorgenForDto(morEllerFar, sammeBosted, harOmsorgenFor);
                }
            }
        }
        return null;
    }

    private Boolean utledOmsorgenFor(Optional<MedisinskGrunnlag> medisinskGrunnlag, boolean morEllerFar, boolean sammeBosted) {
        var saksbehandlersOmsorgenFor = medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null);
        if (morEllerFar && sammeBosted) {
            return true;
        } else if (morEllerFar && Objects.equals(true, saksbehandlersOmsorgenFor)) {
            return true;
        } else if (sammeBosted && Objects.equals(true, saksbehandlersOmsorgenFor)) {
            return true;
        } else if ((morEllerFar || sammeBosted) && saksbehandlersOmsorgenFor == null) {
            return null;
        }
        return false;
    }

    private DatoIntervallEntitet mapTilPeriode(Søknadsperioder fordeling) {
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
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }
}
