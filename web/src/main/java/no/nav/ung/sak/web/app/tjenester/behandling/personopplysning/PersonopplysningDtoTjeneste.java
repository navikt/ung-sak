package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.kontrakt.person.PersonopplysningDto;
import no.nav.ung.sak.web.app.tjenester.behandling.søknad.SøknadDtoFeil;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;

    PersonopplysningDtoTjeneste() {
    }

    @Inject
    public PersonopplysningDtoTjeneste(PersonopplysningTjeneste personopplysningTjeneste,
                                       BehandlingRepository behandlingRepository) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    private static String formaterMedStoreOgSmåBokstaver(String tekst) {
        if (tekst == null || (tekst = tekst.trim()).isEmpty()) { // NOSONAR
            return null;
        }
        String skilletegnPattern = "(\\s|[()\\-_.,/])";
        char[] tegn = tekst.toLowerCase(Locale.getDefault()).toCharArray();
        boolean nesteSkalHaStorBokstav = true;
        for (int i = 0; i < tegn.length; i++) {
            boolean erSkilletegn = String.valueOf(tegn[i]).matches(skilletegnPattern);
            if (!erSkilletegn && nesteSkalHaStorBokstav) {
                tegn[i] = Character.toTitleCase(tegn[i]);
            }
            nesteSkalHaStorBokstav = erSkilletegn;
        }
        return new String(tegn);
    }

    public Optional<PersonopplysningDto> lagPersonopplysningDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<PersonopplysningerAggregat> aggregatOpt = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt);

        if (aggregatOpt.isPresent()) {
            PersonopplysningerAggregat aggregat = aggregatOpt.get();
            return Optional.ofNullable(aggregat.getSøker())
                .map(søker -> mapPersonopplysningDto(søker, aggregat, behandlingId));
        }
        return Optional.empty();
    }

    private PersonopplysningDto mapPersonopplysningDto(PersonopplysningEntitet søker, PersonopplysningerAggregat aggregat, Long behandlingId) {
        PersonopplysningDto dto = enkelMapping(søker, aggregat);
        leggTilRegistrerteBarn(aggregat, dto);
        leggTilEktefelle(søker, aggregat, dto);
        return dto;
    }

    private void leggTilRegistrerteBarn(PersonopplysningerAggregat aggregat, PersonopplysningDto dto) {
        dto.setBarn(aggregat.getBarna()
            .stream()
            .map(e -> enkelMapping(e, aggregat))
            .collect(Collectors.toList()));
    }

    private void leggTilEktefelle(PersonopplysningEntitet søker, PersonopplysningerAggregat aggregat, PersonopplysningDto dto) {
        Optional<PersonopplysningEntitet> ektefelleOpt = aggregat.getEktefelle();
        if (ektefelleOpt.isPresent() && ektefelleOpt.get().equals(søker)) {
            throw SøknadDtoFeil.FACTORY.kanIkkeVæreSammePersonSomSøker().toException();
        }

        if (ektefelleOpt.isPresent()) {
            PersonopplysningDto ektefelle = enkelMapping(ektefelleOpt.get(), aggregat);
            dto.setEktefelle(ektefelle);
        }
    }

    private PersonopplysningDto enkelMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonopplysningDto dto = new PersonopplysningDto();
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
        dto.setAktørId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setDodsdato(personopplysning.getDødsdato());
        dto.setFodselsdato(personopplysning.getFødselsdato());
        return dto;
    }
}
