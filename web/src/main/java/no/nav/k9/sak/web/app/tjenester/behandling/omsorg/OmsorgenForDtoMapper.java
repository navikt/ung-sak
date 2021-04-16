package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForDto;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForOversiktDto;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

@ApplicationScoped
class OmsorgenForDtoMapper {

    private SøknadsperiodeRepository søknadsperiodeRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    OmsorgenForDtoMapper() {
        // CDI
    }

    @Inject
    public OmsorgenForDtoMapper(OmsorgenForGrunnlagRepository omsorgenForRepository, BasisPersonopplysningTjeneste personopplysningTjeneste, SøknadsperiodeRepository søknadsperiodeRepository) {
        this.omsorgenForGrunnlagRepository = omsorgenForRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
    }

    
    public OmsorgenForOversiktDto map(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        final boolean tvingManuellVurdering = false;
        final var systemdata = hentSystemdata(behandlingId, aktørId, optPleietrengendeAktørId);
        
        final Optional<OmsorgenForGrunnlag> grunnlagOpt = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), tvingManuellVurdering, List.of());
        }
        final boolean ikkeVurdertBlirOppfylt = systemdata.isRegistrertForeldrerelasjon() && !tvingManuellVurdering ;
        return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), tvingManuellVurdering, toOmsorgenForDtoListe(grunnlagOpt.get().getOmsorgenFor().getPerioder(), ikkeVurdertBlirOppfylt));
    }

    private Systemdata hentSystemdata(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
    	final Optional<SøknadsperiodeGrunnlag> søknadsgrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId);
        var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
    	if (søknadsgrunnlag.isEmpty()
    			|| søknadsgrunnlag.get().getOppgitteSøknadsperioder() == null
    			|| søknadsgrunnlag.get().getOppgitteSøknadsperioder().getPerioder() == null
    			|| pleietrengende.isEmpty()) {
    		return new Systemdata(false, false);
    	}
        var søknadsperioder = søknadsgrunnlag.get().getOppgitteSøknadsperioder();
        var periode = mapTilPeriode(søknadsperioder.getPerioder());

        var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
        if (!optAggregat.isPresent()) {
            return new Systemdata(false, false);
        }
        var aggregat = optAggregat.get();
        var pleietrengendeAktørId = pleietrengende.get();
        var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

        var registrertForeldrerelasjon = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
        var registrertSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);
        
        return new Systemdata(registrertForeldrerelasjon, registrertSammeBosted);

    }

    private List<OmsorgenForDto> toOmsorgenForDtoListe(List<OmsorgenForPeriode> perioder, boolean ikkeVurdertBlirOppfylt) {
        return perioder.stream()
                .map(p -> new OmsorgenForDto(toPeriode(p), p.getBegrunnelse(), p.getRelasjon(), p.getRelasjonsbeskrivelse(), p.getResultat(), mapResultatEtterAutomatikk(p.getResultat(), ikkeVurdertBlirOppfylt)))
                .collect(Collectors.toList());
    }

    private Resultat mapResultatEtterAutomatikk(Resultat resultat, boolean ikkeVurdertBlirOppfylt) {
        return (resultat == Resultat.IKKE_VURDERT && ikkeVurdertBlirOppfylt) ? Resultat.OPPFYLT : resultat;
    }

    private Periode toPeriode(OmsorgenForPeriode p) {
        return new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato());
    }

    private DatoIntervallEntitet mapTilPeriode(Set<Søknadsperioder> søknadsperioder) {
    	final List<DatoIntervallEntitet> perioder = søknadsperioder.stream()
    			.map(p -> p.getPerioder())
    			.flatMap(Set::stream)
    			.map(s -> s.getPeriode())
    			.collect(Collectors.toList());
    	
        final var fom = perioder
        		.stream()
        		.map(DatoIntervallEntitet::getFomDato)
        		.min(LocalDate::compareTo)
        		.orElseThrow();
        
        final var tom = perioder
        		.stream()
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
