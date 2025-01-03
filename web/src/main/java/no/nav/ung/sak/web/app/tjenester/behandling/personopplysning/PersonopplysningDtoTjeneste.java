package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.kontrakt.person.AvklartPersonstatus;
import no.nav.ung.sak.kontrakt.person.PersonadresseDto;
import no.nav.ung.sak.kontrakt.person.PersonopplysningDto;
import no.nav.ung.sak.typer.AktørId;
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

    private static List<PersonadresseDto> lagAddresseDto(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        List<PersonAdresseEntitet> adresser = aggregat.getAdresserFor(personopplysning.getAktørId());
        return adresser.stream().map(e -> lagDto(e, personopplysning.getNavn())).collect(Collectors.toList());
    }

    private static PersonadresseDto lagDto(PersonAdresseEntitet adresse, String navn) {
        PersonadresseDto dto = new PersonadresseDto();
        dto.setAdresselinje1(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje1()));
        dto.setAdresselinje2(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje2()));
        dto.setAdresselinje3(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje3()));
        dto.setMottakerNavn(formaterMedStoreOgSmåBokstaver(navn));
        dto.setPoststed(formaterMedStoreOgSmåBokstaver(adresse.getPoststed()));
        dto.setPostNummer(adresse.getPostnummer());
        dto.setLand(adresse.getLand());
        dto.setAdresseType(adresse.getAdresseType());
        return dto;

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
        final Optional<Landkoder> landkoder = aggregat.getStatsborgerskapFor(personopplysning.getAktørId()).stream().findFirst().map(StatsborgerskapEntitet::getStatsborgerskap);
        landkoder.ifPresent(dto::setStatsborgerskap);
        var gjeldendePersonstatus = hentPersonstatus(personopplysning, aggregat);
        dto.setPersonstatus(gjeldendePersonstatus);
        var avklartPersonstatus = new AvklartPersonstatus(aggregat.getOrginalPersonstatusFor(personopplysning.getAktørId())
            .map(PersonstatusEntitet::getPersonstatus).orElse(gjeldendePersonstatus),
            gjeldendePersonstatus);
        dto.setAvklartPersonstatus(avklartPersonstatus);
        dto.setSivilstand(personopplysning.getSivilstand());
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
        dto.setAktørId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setDodsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));

        if (personopplysning.getRegion() != null) {
            dto.setRegion(personopplysning.getRegion());
        }
        dto.setFodselsdato(personopplysning.getFødselsdato());
        return dto;
    }

    private PersonstatusType hentPersonstatus(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonstatusEntitet personstatus = aggregat.getPersonstatusFor(personopplysning.getAktørId());
        if (personstatus == null) {
            return PersonstatusType.UDEFINERT;
        }
        return personstatus.getPersonstatus();
    }
}
