package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
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
        final var systemdata = hentSystemdata(behandlingId, aktørId, optPleietrengendeAktørId);
        
        final Optional<OmsorgenForGrunnlag> grunnlagOpt = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), false, List.of());
        }
        
        return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), false, toOmsorgenForDtoListe(grunnlagOpt.get().getOmsorgenFor().getPerioder()));
    }

    private Systemdata hentSystemdata(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isPresent()) {
            var periode = mapTilPeriode(søknadsperioder.get());
            var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
            if (pleietrengende.isPresent()) {
                var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
                if (optAggregat.isPresent()) {
                    var aggregat = optAggregat.get();
                    var pleietrengendeAktørId = pleietrengende.get();
                    var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

                    var registrertForeldrerelasjon = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
                    var registrertSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);
                    
                    return new Systemdata(registrertForeldrerelasjon, registrertSammeBosted);
                }
            }
        }
        return new Systemdata(false, false);
    }

    private List<OmsorgenForDto> toOmsorgenForDtoListe(List<OmsorgenForPeriode> perioder) {
        return perioder.stream()
                .map(p -> new OmsorgenForDto(toPeriode(p), p.getBegrunnelse(), p.getRelasjon(), p.getRelasjonsbeskrivelse(), p.getResultat()))
                .collect(Collectors.toList());
    }

    private Periode toPeriode(OmsorgenForPeriode p) {
        return new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato());
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
    
    private static class Systemdata {
        private final boolean registrertForeldrerelasjon;
        private final boolean registrertSammeBosted;
        
        
        public Systemdata(boolean registrertForeldrerelasjon, boolean registrertSammeBosted) {
            this.registrertForeldrerelasjon = registrertForeldrerelasjon;
            this.registrertSammeBosted = registrertSammeBosted;
        }
        
        
        public boolean isRegistrertForeldrerelasjon() {
            return registrertForeldrerelasjon;
        }
        
        public boolean isRegistrertSammeBosted() {
            return registrertSammeBosted;
        }
    }
}
