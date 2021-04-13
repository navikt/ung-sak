package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.medisinsk.OmsorgenForDto;
import no.nav.k9.sak.kontrakt.medisinsk.OmsorgenForOversiktDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForPeriode;

@ApplicationScoped
class OmsorgenForDtoMapper {

    private UttakRepository uttakRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    OmsorgenForDtoMapper() {
        // CDI
    }

    @Inject
    public OmsorgenForDtoMapper(UttakRepository uttakRepository, OmsorgenForGrunnlagRepository omsorgenForRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.uttakRepository = uttakRepository;
        this.omsorgenForGrunnlagRepository = omsorgenForRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    
    public OmsorgenForOversiktDto map(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        /* TODO Omsorg: Skal vi ta med informasjon fra PDL?
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isPresent()) {
            var periode = mapTilPeriode(søknadsperioder.get());
            var omsorgenForGrunnlag = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
            var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
            if (pleietrengende.isPresent()) {
                var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
                if (optAggregat.isPresent()) {
                    var aggregat = optAggregat.get();
                    var pleietrengendeAktørId = pleietrengende.get();

                    var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

                    var morEllerFar = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
                    var sammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);
                    var harOmsorgenFor = utledOmsorgenFor(omsorgenForGrunnlag, morEllerFar, sammeBosted);

                    return new OmsorgenForDto(morEllerFar, sammeBosted, harOmsorgenFor);
                }
            }
        }
        */
        final Optional<OmsorgenForGrunnlag> grunnlagOpt = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return new OmsorgenForOversiktDto(List.of());
        }
        
        return new OmsorgenForOversiktDto(toOmsorgenForDtoListe(grunnlagOpt.get().getOmsorgenFor().getPerioder()));
    }

    private List<OmsorgenForDto> toOmsorgenForDtoListe(List<OmsorgenForPeriode> perioder) {
        return perioder.stream()
                .map(p -> new OmsorgenForDto(toPeriode(p), p.getBegrunnelse(), p.getRelasjon(), p.getRelasjonsbeskrivelse(), p.getResultat()))
                .collect(Collectors.toList());
    }

    private Periode toPeriode(OmsorgenForPeriode p) {
        return new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato());
    }

    /*
    private Boolean utledOmsorgenFor(Optional<OmsorgenForGrunnlag> omsorgenForRepository, boolean morEllerFar, boolean sammeBosted) {
        var saksbehandlersOmsorgenFor = omsorgenForRepository.map(OmsorgenForGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null);
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
    */
}
