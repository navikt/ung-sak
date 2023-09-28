package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.AktørInntekt;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOgArbeidsforholdOpplysningerDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOgArbeidsforholdOversiktDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOpplysningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.k9.sak.typer.Arbeidsgiver;

@Dependent
public class ArbeidsgiverOversiktTjeneste {

    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @Inject
    public ArbeidsgiverOversiktTjeneste(BehandlingRepository behandlingRepository, InntektArbeidYtelseTjeneste iayTjeneste, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public ArbeidsgiverOgArbeidsforholdOversiktDto getArbeidsgiverOgArbeidsforholdOpplysninger(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);

        Set<Arbeidsgiver> arbeidsgivere = new HashSet<>();

        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        grunnlag.ifPresent(iayg -> {
            iayg.getAktørArbeidFraRegister(behandling.getAktørId())
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList())
                .stream()
                .map(Yrkesaktivitet::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getBekreftetAnnenOpptjening(behandling.getAktørId())
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList())
                .stream()
                .map(Yrkesaktivitet::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getAktørInntektFraRegister(behandling.getAktørId())
                .map(AktørInntekt::getInntekt)
                .orElse(Collections.emptyList())
                .stream()
                .map(Inntekt::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getAktørYtelseFraRegister(behandling.getAktørId())
                .map(AktørYtelse::getAlleYtelser)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(y -> y.getYtelseGrunnlag().stream())
                .flatMap(g -> g.getYtelseStørrelse().stream())
                .flatMap(s -> s.getVirksomhet().stream().map(Arbeidsgiver::virksomhet))
                .forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdInformasjon()
                .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
                .orElse(Collections.emptyList())
                .stream()
                .map(ArbeidsforholdReferanse::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdOverstyringer()
                .stream()
                .map(ArbeidsforholdOverstyring::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getOppgittOpptjening()
                .map(OppgittOpptjening::getEgenNæring)
                .orElse(Collections.emptyList())
                .stream()
                .map(OppgittEgenNæring::getVirksomhetOrgnr)
                .filter(Objects::nonNull)
                .map(Arbeidsgiver::virksomhet)
                .forEach(arbeidsgivere::add);
            // MERK: Tar ikke hensyn til vilkårsperioder under vurdering dersom man henter ut aggregat for oppgitt opptjening
            iayg.getOppgittOpptjeningAggregat()
                .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
                .orElse(List.of())
                .stream()
                .map(OppgittOpptjening::getEgenNæring)
                .flatMap(Collection::stream)
                .map(OppgittEgenNæring::getVirksomhetOrgnr)
                .filter(Objects::nonNull)
                .map(Arbeidsgiver::virksomhet)
                .forEach(arbeidsgivere::add);
        });

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(behandling.getFagsak().getSaksnummer());
        inntektsmeldinger.stream()
            .map(Inntektsmelding::getArbeidsgiver)
            .forEach(arbeidsgivere::add);

        Map<String, ArbeidsgiverOgArbeidsforholdOpplysningerDto> oversikt = new HashMap<>();

        var referanser = grunnlag.flatMap(gr -> gr.getArbeidsforholdInformasjon().map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser))
            .orElse(Collections.emptyList());

        arbeidsgivere.stream()
            .map(a -> this.mapFra(a, referanser))
            .collect(Collectors.groupingBy(ArbeidsgiverOgArbeidsforholdOpplysningerDto::getIdentifikator))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOgArbeidsforholdOpplysningerDto(key, "Ukjent", mapReferanserForIdentifikator(referanser, key)))));
        return new ArbeidsgiverOgArbeidsforholdOversiktDto(oversikt);
    }

    private ArbeidsgiverOgArbeidsforholdOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver, Collection<ArbeidsforholdReferanse> referanser) {
        var identifikator = arbeidsgiver.getIdentifikator();
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            List<ArbeidsforholdIdDto> arbeidsforholdreferanser = mapReferanserForIdentifikator(referanser, identifikator);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOgArbeidsforholdOpplysningerDto(identifikator, opplysninger.getNavn(), arbeidsforholdreferanser);
            } else {
                return new ArbeidsgiverOgArbeidsforholdOpplysningerDto(identifikator, opplysninger.getAlternativIdentifikator(),
                    opplysninger.getNavn(), opplysninger.getFødselsdato(), arbeidsforholdreferanser);
            }
        } catch (Exception e) {
            return new ArbeidsgiverOgArbeidsforholdOpplysningerDto(identifikator, "Feil ved oppslag", mapReferanserForIdentifikator(referanser, identifikator));
        }
    }

    private List<ArbeidsforholdIdDto> mapReferanserForIdentifikator(Collection<ArbeidsforholdReferanse> referanser, String identifikator) {
        return referanser.stream()
            .filter(r -> r.getArbeidsgiver().getIdentifikator().equals(identifikator))
            .map(r -> new ArbeidsforholdIdDto(r.getInternReferanse().getUUIDReferanse(), r.getEksternReferanse().getReferanse()))
            .toList();
    }


    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysning(Collection<Arbeidsgiver> arbeidsgivere) {
        Map<String, ArbeidsgiverOpplysningDto> oversikt = new HashMap<>();

        arbeidsgivere.stream()
            .map(this::mapFra)
            .collect(Collectors.groupingBy(ArbeidsgiverOpplysningDto::getIdentifikator))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOpplysningDto(key, "Ukjent"))));

        return new ArbeidsgiverOversiktDto(oversikt);

    }

    private ArbeidsgiverOpplysningDto mapFra(Arbeidsgiver arbeidsgiver) {
        var identifikator = arbeidsgiver.getIdentifikator();
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningDto(identifikator, opplysninger.getNavn());
            } else {
                return new ArbeidsgiverOpplysningDto(identifikator, opplysninger.getAlternativIdentifikator(), opplysninger.getNavn(), opplysninger.getFødselsdato());
            }
        } catch (Exception e) {
            return new ArbeidsgiverOpplysningDto(identifikator, "Feil ved oppslag");
        }
    }


}
