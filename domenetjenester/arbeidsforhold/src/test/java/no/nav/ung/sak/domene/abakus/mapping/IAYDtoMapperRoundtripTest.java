package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.*;
import no.nav.abakus.iaygrunnlag.inntekt.v1.InntekterDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingsPostDto;
import no.nav.abakus.iaygrunnlag.kodeverk.*;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.*;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatRegisterDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.*;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IAYDtoMapperRoundtripTest {

    private final UUID uuid = UUID.randomUUID();
    private final LocalDate fom = LocalDate.now();
    private final LocalDate tom = LocalDate.now();
    private final AktørIdPersonident aktørIdent = new AktørIdPersonident("9912341234123");
    private final AktørId aktørId = new AktørId(aktørIdent.getIdent());
    private final Organisasjon org = new Organisasjon("974760673");
    private final ArbeidType arbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
    private final Periode periode = new Periode(fom, tom);
    private final YtelseType ytelseType = YtelseType.FORELDREPENGER;
    private final LocalDateTime tidspunkt = LocalDateTime.now();
    private final OffsetDateTime offTidspunkt = OffsetDateTime.now();
    private final JournalpostId journalpostId = new JournalpostId("ImajournalpostId");
    private final ArbeidsforholdRefDto arbeidsforholdId = new ArbeidsforholdRefDto(InternArbeidsforholdRef.nyRef().getReferanse(), "aaregRef");

    private final IAYFraDtoMapper fraDtoMapper = new IAYFraDtoMapper(aktørId);

    @Test
    public void roundtrip_mapping_dto_til_grunnlag_til_dto() {
        // Arrange
        InntektArbeidYtelseGrunnlagDto dto = lagIAYGrunnlag();

        // Act
        InntektArbeidYtelseGrunnlag iayGrunnlag = fraDtoMapper.mapTilGrunnlagInklusivRegisterdata(dto, true);
        IAYTilDtoMapper dtoMapper = new IAYTilDtoMapper(aktørId, uuid, uuid);
        InntektArbeidYtelseGrunnlagDto dtoIgjen = dtoMapper.mapTilDto(ytelseType, iayGrunnlag);

        // Assert
        assertThat(dtoIgjen.getGrunnlagTidspunkt()).isEqualTo(dto.getGrunnlagTidspunkt());
        assertThat(dtoIgjen.getGrunnlagReferanse()).isEqualTo(dto.getGrunnlagReferanse());
        assertThat(dtoIgjen.getKoblingReferanse()).isEqualTo(dto.getKoblingReferanse());
        assertThat(dtoIgjen.getOppgittOpptjening()).isEqualToComparingFieldByFieldRecursively(dto.getOppgittOpptjening());
        assertThat(dtoIgjen.getPerson()).isEqualToComparingFieldByFieldRecursively(dto.getPerson());

        assertThat(dtoIgjen.getOverstyrt().getOpprettetTidspunkt()).isEqualTo(dto.getOverstyrt().getOpprettetTidspunkt());

        assertThat(dtoIgjen.getOverstyrt()).isEqualToComparingFieldByFieldRecursively(dto.getOverstyrt());

        assertThat(dtoIgjen.getRegister()).isEqualToComparingFieldByFieldRecursively(dto.getRegister());

        // alla i hopa
        //assertThat(dtoIgjen).isEqualToComparingFieldByFieldRecursively(dto);

    }

    private InntektArbeidYtelseGrunnlagDto lagIAYGrunnlag() {
        InntektArbeidYtelseGrunnlagDto grunnlag = new InntektArbeidYtelseGrunnlagDto(aktørIdent, offTidspunkt, uuid, uuid, ytelseType);

        grunnlag.medRegister(
                new InntektArbeidYtelseAggregatRegisterDto(tidspunkt, uuid)
                    .medInntekt(List.of(
                        new InntekterDto(aktørIdent)
                            .medUtbetalinger(List.of(
                                new UtbetalingDto("INNTEKT_BEREGNING")
                                    .medArbeidsgiver(org)
                                    .medPoster(List.of(
                                        new UtbetalingsPostDto(periode, InntektspostType.LØNN)
                                            .medInntektYtelseType(InntektYtelseType.FORELDREPENGER)
                                            .medBeløp(100)
                                            .medSkattAvgiftType(SkatteOgAvgiftsregelType.NETTOLØNN)))))))
                    .medYtelse(List.of(
                        new YtelserDto(aktørIdent)
                            .medYtelser(List.of(
                                new YtelseDto(Fagsystem.FPSAK, ytelseType, periode, YtelseStatus.LØPENDE)
                                    .medSaksnummer("1234")
                                    .medGrunnlag(
                                        new YtelseGrunnlagDto()
                                            .medArbeidskategoriDto(Arbeidskategori.ARBEIDSTAKER)
                                            .medOpprinneligIdentDato(fom)
                                            .medDekningsgradProsent(100)
                                            .medInntektsgrunnlagProsent(100)
                                            .medGraderingProsent(100)
                                            .medFordeling(List.of(new FordelingDto(org, InntektPeriodeType.DAGLIG, 100, true))))
                                    .medAnvisninger(List.of(
                                        new AnvisningDto(periode)
                                            .medBeløp(100)
                                            .medDagsats(100)
                                            .medUtbetalingsgrad(100))))))))
            .medOverstyrt(
                new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, uuid))
            .medOppgittOpptjening(
                new OppgittOpptjeningDto(null, null, uuid, offTidspunkt)
                    .medArbeidsforhold(List.of(
                        new OppgittArbeidsforholdDto(periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                            .medErUtenlandskInntekt(true)
                            .medOppgittVirksomhetNavn("GammelDansk", Landkode.DNK)))
                    .medEgenNæring(List.of(
                        new OppgittEgenNæringDto(periode)
                            .medBegrunnelse("MinBegrunnelse")
                            .medBruttoInntekt(10000)
                            .medEndringDato(fom)
                            .medNyIArbeidslivet(false)
                            .medNyoppstartet(false)
                            .medNærRelasjon(false)
                            .medOppgittVirksomhetNavn("DuGamleDuFria", Landkode.SWE)
                            .medRegnskapsførerNavn("Regnskapsfører")
                            .medRegnskapsførerTlf("+47902348732")
                            .medVarigEndring(true)
                            .medVirksomhet(org)
                            .medVirksomhetType(VirksomhetType.ANNEN)))
                    .medAnnenAktivitet(List.of(new OppgittAnnenAktivitetDto(periode, arbeidType)))
                    .medFrilans(new OppgittFrilansDto(List.of(
                        new OppgittFrilansoppdragDto(periode, "MittOppdrag")))
                        .medErNyoppstartet(false)
                        .medHarInntektFraFosterhjem(false)
                        .medHarNærRelasjon(false)));

        return grunnlag;

    }

}
