package no.nav.k9.sak.web.app.tjenester.behandling.opptjening;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.kontrakt.opptjening.InntektDto;
import no.nav.k9.sak.kontrakt.opptjening.InntekterDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

@Dependent
class MapInntekter {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @Inject
    MapInntekter(InntektArbeidYtelseTjeneste iayTjeneste, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    private List<InntektDto> finnPgiInntekterFørStp(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId, LocalDate førDato) {
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(førDato).filterPensjonsgivende();
        List<InntektDto> inntektDto = mapAktørInntekt(filter);
        return inntektDto;
    }

    private List<InntektDto> mapAktørInntekt(InntektFilter filter) {
        List<InntektDto> resultat = new ArrayList<>();
        filter.forFilter((inntekt, inntektsposter) -> resultat.addAll(mapInntekt(inntekt.getArbeidsgiver(), inntektsposter)));
        return resultat;
    }

    private List<InntektDto> mapInntekt(Arbeidsgiver arbeidsgiver, Collection<Inntektspost> inntektsposter) {
        String utbetaler = finnUtbetalerVisningstekst(arbeidsgiver);
        List<InntektDto> inntektDto = inntektsposter.stream().map(ip -> mapInntektspost(utbetaler, ip)).collect(Collectors.toList());
        return inntektDto;
    }

    private InntektDto mapInntektspost(String utbetaler, Inntektspost inntektspost) {
        InntektDto dto = new InntektDto(); // NOSONAR
        if (utbetaler != null) {
            dto.setUtbetaler(utbetaler);
        } else {
            if (inntektspost.getYtelseType() != null) {
                dto.setUtbetaler(inntektspost.getYtelseType().getNavn());
            }
        }
        dto.setFom(inntektspost.getPeriode().getFomDato());
        dto.setTom(inntektspost.getPeriode().getTomDato());
        dto.setYtelse(inntektspost.getInntektspostType().equals(InntektspostType.YTELSE));
        dto.setInntektspostType(inntektspost.getInntektspostType());
        dto.setYtelseType(inntektspost.getYtelseType());
        dto.setBelop(inntektspost.getBeløp().getVerdi().intValue());
        return dto;
    }

    private String finnUtbetalerVisningstekst(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.erAktørId()) {
            return lagPrivatpersontekst(arbeidsgiver);
        } else {
            return arbeidsgiver.getIdentifikator();
        }
    }

    private String lagPrivatpersontekst(Arbeidsgiver arbeidsgiver) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger.getNavn() == null) {
            return "UKJENT NAVN";
        }
        String navn = opplysninger.getNavn();
        String avkortetNavn = navn.length() < 5 ? navn : navn.substring(0, 5);
        String formatertFødselsdato = opplysninger.getFødselsdato() != null
            ? opplysninger.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            : opplysninger.getIdentifikator();
        return avkortetNavn + "..." + "(" + formatertFødselsdato + ")";
    }

    InntekterDto hentPgiInntekterFørStp(BehandlingReferanse ref, LocalDate førDato) {
        var inntekter = iayTjeneste.finnGrunnlag(ref.getBehandlingId())
            .map(aggregat -> finnPgiInntekterFørStp(aggregat, ref.getAktørId(), førDato)).orElse(Collections.emptyList());
        return new InntekterDto(inntekter);
    }

}
