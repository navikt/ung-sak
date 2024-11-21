package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatRegisterDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.typer.AktørId;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public class IAYTilDtoMapper {

    private final AktørId aktørId;
    private final UUID grunnlagReferanse;
    private final UUID behandlingReferanse;

    public IAYTilDtoMapper(AktørId aktørId,
                           UUID grunnlagReferanse,
                           UUID behandlingReferanse) {
        this.aktørId = aktørId;
        this.grunnlagReferanse = grunnlagReferanse;
        this.behandlingReferanse = behandlingReferanse;
    }

    public OverstyrtInntektArbeidYtelseDto mapTilDto(FagsakYtelseType fagsakYtelseType, InntektArbeidYtelseAggregat overstyrt, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var ytelseType = YtelseType.fraKode(fagsakYtelseType.getKode());
        var person = new AktørIdPersonident(aktørId.getId());
        var arbeidsforholdInformasjonDto = new MapArbeidsforholdInformasjon.MapTilDto().map(arbeidsforholdInformasjon, grunnlagReferanse, true);
        var overstyrtDto = mapSaksbehandlerOverstyrteOpplysninger(arbeidsforholdInformasjon, overstyrt);
        return new OverstyrtInntektArbeidYtelseDto(person, grunnlagReferanse, behandlingReferanse, ytelseType, arbeidsforholdInformasjonDto, overstyrtDto);
    }

    public InntektArbeidYtelseGrunnlagDto mapTilDto(YtelseType ytelseType, InntektArbeidYtelseGrunnlag grunnlag, boolean validerArbeidsforholdId) {
        if (grunnlag == null) {
            return null;
        }

        var grunnlagTidspunkt = grunnlag.getOpprettetTidspunkt().atZone(ZoneId.systemDefault()).toOffsetDateTime();

        var dto = new InntektArbeidYtelseGrunnlagDto(
            new AktørIdPersonident(aktørId.getId()),
            grunnlagTidspunkt,
            grunnlagReferanse,
            behandlingReferanse,
            ytelseType);

        // REGISTEROPPLYSNINGER
        grunnlag.getRegisterVersjon().ifPresent(a -> mapRegisterOpplysninger(getArbeidforholdInfo(grunnlag), a, dto));

        // SAKSBEHANDLER OVERSTYRTE OPPLYSNINGER (ARBEIDSFORHOLD)
        grunnlag.getArbeidsforholdInformasjon().ifPresent(ai -> {
            var arbeidsforholdInformasjon = new MapArbeidsforholdInformasjon.MapTilDto().map(ai, grunnlag.getEksternReferanse(), grunnlag.isAktiv());
            dto.medArbeidsforholdInformasjon(arbeidsforholdInformasjon);
        });
        grunnlag.getSaksbehandletVersjon().ifPresent(a -> dto.medOverstyrt(mapSaksbehandlerOverstyrteOpplysninger(getArbeidforholdInfo(grunnlag), a)));

        // OPPGITT OPPTJENING
        grunnlag.getOppgittOpptjening().ifPresent(oo -> dto.medOppgittOpptjening(new MapOppgittOpptjening().mapTilDto(oo)));
        grunnlag.getOppgittOpptjeningAggregat().ifPresent( oa -> dto.medOppgittOpptjeninger(new MapOppgittOpptjening().mapTilDto(oa)));
        return dto;
    }

    public OppgittOpptjeningDto mapTilDto(OppgittOpptjeningBuilder builder) {
        return new MapOppgittOpptjening().mapTilDto(builder.build());
    }

    private ArbeidsforholdInformasjon getArbeidforholdInfo(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getArbeidsforholdInformasjon().orElseThrow(() -> new IllegalStateException("Mangler ArbeidsforholdInformasjon i grunnlag (påkrevd her): " + grunnlag.getEksternReferanse()));
    }

    private void mapRegisterOpplysninger(ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                         InntektArbeidYtelseAggregat aggregat,
                                         InntektArbeidYtelseGrunnlagDto dto) {
        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt())
            .map(it -> it.atZone(ZoneId.systemDefault()).toOffsetDateTime())
            .orElse(OffsetDateTime.now());

        var arbeid = new MapAktørArbeid.MapTilDto(arbeidsforholdInformasjon).map(aggregat.getAktørArbeid());
        var inntekter = new MapAktørInntekt.MapTilDto().map(aggregat.getAktørInntekt());
        var ytelser = new MapAktørYtelse.MapTilDto().map(aggregat.getAktørYtelse());
        var register = new InntektArbeidYtelseAggregatRegisterDto(tidspunkt, aggregat.getEksternReferanse())
            .medArbeid(arbeid)
            .medInntekt(inntekter)
            .medYtelse(ytelser);
        dto.medRegister(register);
    }

    private InntektArbeidYtelseAggregatOverstyrtDto mapSaksbehandlerOverstyrteOpplysninger(ArbeidsforholdInformasjon arbeidsforholdInformasjon, InntektArbeidYtelseAggregat aggregat) {
        if (aggregat == null) {
            return null;
        }

        var tidspunkt = Optional.ofNullable(aggregat.getOpprettetTidspunkt())
            .map(it -> it.atZone(ZoneId.systemDefault()).toOffsetDateTime())
            .orElse(OffsetDateTime.now());

        var aktørArbeid = aggregat.getAktørArbeid();
        var arbeid = new MapAktørArbeid.MapTilDto(arbeidsforholdInformasjon).map(aktørArbeid);
        var overstyrt = new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, aggregat.getEksternReferanse());
        overstyrt.medArbeid(arbeid);

        return overstyrt;
    }

}
