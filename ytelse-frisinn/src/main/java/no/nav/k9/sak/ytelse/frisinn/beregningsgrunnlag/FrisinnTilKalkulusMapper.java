package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.Collections;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

/**
 * Mapper fra k9-format til kalkulus-format, benytter kontrakt v1 fra kalkulus
 */
public class FrisinnTilKalkulusMapper extends TilKalkulusMapper {

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
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(oppgittOpptjening));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));

        return inntektArbeidYtelseGrunnlagDto;
    }

}
