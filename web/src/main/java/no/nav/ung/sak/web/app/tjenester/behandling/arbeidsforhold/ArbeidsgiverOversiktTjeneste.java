package no.nav.ung.sak.web.app.tjenester.behandling.arbeidsforhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.domene.iay.modell.Inntekt;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.Inntekter;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsgiverOpplysningerDto;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.ung.sak.felles.typer.Arbeidsgiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class ArbeidsgiverOversiktTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ArbeidsgiverOversiktTjeneste.class);
    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @Inject
    public ArbeidsgiverOversiktTjeneste(BehandlingRepository behandlingRepository, InntektArbeidYtelseTjeneste iayTjeneste, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }


    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);

        Set<Arbeidsgiver> arbeidsgivere = new HashSet<>();

        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        grunnlag.ifPresent(iayg -> {
            iayg.getInntekterFraRegister()
                .stream()
                .map(Inntekter::getInntekt)
                .flatMap(Collection::stream)
                .map(Inntekt::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
        });

        Map<String, ArbeidsgiverOpplysningerDto> oversikt = new HashMap<>();


        arbeidsgivere.stream()
            .map(this::mapFra)
            .collect(Collectors.groupingBy(ArbeidsgiverOpplysningerDto::getIdentifikator))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOpplysningerDto(key, "Ukjent"))));
        return new ArbeidsgiverOversiktDto(oversikt);
    }

    private ArbeidsgiverOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver) {
        var identifikator = arbeidsgiver.getIdentifikator();
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getNavn());
            } else {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getAlternativIdentifikator(),
                    opplysninger.getNavn(), opplysninger.getFødselsdato());
            }
        } catch (Exception e) {
            log.warn("Feil ved oppslag av arbeidsgiveropplysninger for identifikator {}", identifikator, e);
            return new ArbeidsgiverOpplysningerDto(identifikator, "Feil ved oppslag");
        }
    }



}
