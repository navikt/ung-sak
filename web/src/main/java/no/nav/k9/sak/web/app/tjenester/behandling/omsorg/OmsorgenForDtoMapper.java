package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Pleietrengende;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
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

    public OmsorgenForDto map(Long behandlingId, AktørId aktørId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isPresent()) {
            var periode = mapTilPeriode(søknadsperioder.get());
            var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
            var pleietrengende = medisinskGrunnlag.map(MedisinskGrunnlag::getPleietrengende);
            if (pleietrengende.isPresent()) {
                var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
                if (optAggregat.isPresent()) {
                    var aggregat = optAggregat.get();
                    var pleietrengendeAktørId = pleietrengende.map(Pleietrengende::getAktørId).get();

                    var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

                    var morEllerFar = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
                    var sammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);

                    return new OmsorgenForDto(morEllerFar, sammeBosted);
                }
            }
        }
        return null;
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
