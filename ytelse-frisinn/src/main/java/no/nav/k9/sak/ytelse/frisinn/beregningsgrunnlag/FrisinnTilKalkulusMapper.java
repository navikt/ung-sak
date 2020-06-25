package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansInntekt;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

/**
 * Mapper fra k9-format til kalkulus-format, benytter kontrakt v1 fra kalkulus
 *
 * Mapper næring, frilans og oppgitt arbeidsforhold som overlapper med angitt vilkårsperiode
 *
 * Mapper ingen inntektsmeldinger
 *
 * Bruker inntekter fram til siste dag i vilkårsperioden
 *
 */
public class FrisinnTilKalkulusMapper extends TilKalkulusMapper {

    public static final LocalDate STP_FRISINN = LocalDate.of(2020, 3, 1);

    @Override
    public InntektArbeidYtelseGrunnlagDto mapTilDto(InntektArbeidYtelseGrunnlag grunnlag,
                                                    SakInntektsmeldinger sakInntektsmeldinger,
                                                    AktørId aktørId,
                                                    DatoIntervallEntitet vilkårsPeriode,
                                                    OppgittOpptjening oppgittOpptjening) {
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(vilkårsPeriode.getTomDato());
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        Optional<AktørArbeid> arbeid = grunnlag.getAktørArbeidFraRegister(aktørId);

        var yrkesaktiviteterForBeregning = arbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList());
        var alleRelevanteInntekter = finnRelevanteInntekter(inntektFilter);
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(yrkesaktiviteterForBeregning));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(alleRelevanteInntekter));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelseDto(ytelseFilter.getAlleYtelser()));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(oppgittOpptjening, vilkårsPeriode));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));

        return inntektArbeidYtelseGrunnlagDto;
    }

    @Override
    protected List<OppgittEgenNæringDto> mapOppgittEgenNæringListe(List<OppgittEgenNæring> egenNæring, DatoIntervallEntitet vilkårsPeriode) {
        return egenNæring == null ? null : egenNæring.stream()
            .filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().getTomDato().isBefore(STP_FRISINN) || oppgittEgenNæring.getPeriode().overlapper(vilkårsPeriode))
            .map(TilKalkulusMapper::mapOppgittEgenNæring).collect(Collectors.toList());
    }

    @Override
    protected OppgittFrilansDto mapOppgittFrilansOppdragListe(OppgittFrilans oppgittFrilans, DatoIntervallEntitet vilkårsPeriode) {
        List<OppgittFrilansInntekt> oppdrag = oppgittFrilans.getFrilansoppdrag()
            .stream()
            .filter(frilansoppdrag -> frilansoppdrag.getPeriode().overlapper(vilkårsPeriode))
            .map(mapFrilansOppdrag())
            .collect(Collectors.toList());
        return new OppgittFrilansDto(oppgittFrilans.getErNyoppstartet() == null ? false : oppgittFrilans.getErNyoppstartet(), oppdrag);
    }

    @Override
    protected List<OppgittArbeidsforholdDto> mapOppgittArbeidsforholdDto(List<OppgittArbeidsforhold> arbeidsforhold, DatoIntervallEntitet vilkårsPeriode) {
        if (arbeidsforhold == null) {
            return null;
        }
        return arbeidsforhold.stream().filter(a -> a.getPeriode().overlapper(vilkårsPeriode)).map(TilKalkulusMapper::mapArbeidsforhold).collect(Collectors.toList());
    }


}
