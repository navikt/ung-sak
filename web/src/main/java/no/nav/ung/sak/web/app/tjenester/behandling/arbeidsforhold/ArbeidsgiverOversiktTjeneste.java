package no.nav.ung.sak.web.app.tjenester.behandling.arbeidsforhold;

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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.domene.iay.modell.AktørArbeid;
import no.nav.ung.sak.domene.iay.modell.AktørInntekt;
import no.nav.ung.sak.domene.iay.modell.AktørYtelse;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.ung.sak.domene.iay.modell.Inntekt;
import no.nav.ung.sak.domene.iay.modell.Inntektsmelding;
import no.nav.ung.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.ung.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsgiverOpplysningerDto;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.ung.sak.typer.Arbeidsgiver;

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


    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(UUID behandlingUuid) {
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

        Map<String, ArbeidsgiverOpplysningerDto> oversikt = new HashMap<>();

        var referanser = grunnlag.flatMap(gr -> gr.getArbeidsforholdInformasjon().map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser))
            .orElse(Collections.emptyList());

        arbeidsgivere.stream()
            .map(a -> this.mapFra(a, referanser))
            .collect(Collectors.groupingBy(ArbeidsgiverOpplysningerDto::getIdentifikator))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOpplysningerDto(key, "Ukjent", mapReferanserForIdentifikator(referanser, key)))));
        return new ArbeidsgiverOversiktDto(oversikt);
    }

    private ArbeidsgiverOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver, Collection<ArbeidsforholdReferanse> referanser) {
        var identifikator = arbeidsgiver.getIdentifikator();
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            List<ArbeidsforholdIdDto> arbeidsforholdreferanser = mapReferanserForIdentifikator(referanser, identifikator);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getNavn(), arbeidsforholdreferanser);
            } else {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getAlternativIdentifikator(),
                    opplysninger.getNavn(), opplysninger.getFødselsdato(), arbeidsforholdreferanser);
            }
        } catch (Exception e) {
            return new ArbeidsgiverOpplysningerDto(identifikator, "Feil ved oppslag", mapReferanserForIdentifikator(referanser, identifikator));
        }
    }

    private List<ArbeidsforholdIdDto> mapReferanserForIdentifikator(Collection<ArbeidsforholdReferanse> referanser, String identifikator) {
        return referanser.stream()
            .filter(r -> r.getArbeidsgiver().getIdentifikator().equals(identifikator))
            .map(r -> new ArbeidsforholdIdDto(r.getInternReferanse().getUUIDReferanse(), r.getEksternReferanse().getReferanse()))
            .toList();
    }

}
