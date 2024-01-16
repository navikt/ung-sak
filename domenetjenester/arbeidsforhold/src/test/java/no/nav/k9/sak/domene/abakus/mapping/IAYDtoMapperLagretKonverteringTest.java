package no.nav.k9.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.*;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.*;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class IAYDtoMapperLagretKonverteringTest {

    private static final LocalDate DATO = LocalDate.now();
    private static final String ORGNR = "974760673";
    private static final LocalDate FOM_DATO = DATO.minusDays(3);
    private static final LocalDate TOM_DATO = DATO.minusDays(2);
    private static final LocalDate ANVIST_FOM = DATO.minusDays(200);
    private static final LocalDate ANVIST_TOM = DATO.minusDays(100);
    private static final LocalDate OPPRINNELIG_IDENTDATO = DATO.minusDays(100);

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        return iayTjeneste.hentGrunnlag(behandlingId);
    }

    @Test
    public void skal_lagre_ned_inntekt_arbeid_ytelser_og_konvertere_opphentet_til_dto() throws Exception {
        long behandlingId = 1L;
        UUID behandlingUuid = UUID.randomUUID();
        var aktørId = AktørId.dummy();

        var aggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        var aktørInntektBuilder = aggregatBuilder.getAktørInntektBuilder(aktørId);
        var inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
            new Opptjeningsnøkkel(null, ORGNR, null));
        var inntektspostBuilder = inntektBuilder.getInntektspostBuilder();

        var aktørArbeidBuilder = aggregatBuilder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(null, ORGNR, null),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(aktørId);
        aktørYtelseBuilder.leggTilYtelse(lagYtelse());

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var fraOgMed = DATO.minusWeeks(1);
        var tilOgMed = DATO.plusMonths(1);

        var permisjon = permisjonBuilder
            .medProsentsats(BigDecimal.valueOf(100))
            .medPeriode(fraOgMed, tilOgMed)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMISJON)
            .build();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
            .medSisteLønnsendringsdato(fraOgMed);

        var yrkesaktivitet = yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilPermisjon(permisjon)
            .build();

        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        var inntektspost = inntektspostBuilder
            .medBeløp(BigDecimal.TEN)
            .medPeriode(fraOgMed, tilOgMed)
            .medInntektspostType(InntektspostType.YTELSE)
            .medInntektYtelse(InntektYtelseType.OMSORGSPENGER);

        inntektBuilder
            .leggTilInntektspost(inntektspost)
            .medArbeidsgiver(yrkesaktivitet.getArbeidsgiver())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = aktørInntektBuilder
            .leggTilInntekt(inntektBuilder);

        aggregatBuilder.leggTilAktørInntekt(aktørInntekt);
        aggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);

        iayTjeneste.lagreIayAggregat(behandlingId, aggregatBuilder);

        var grunnlag = hentGrunnlag(behandlingId);

        var mapper = new IAYTilDtoMapper(aktørId, grunnlag.getEksternReferanse(), behandlingUuid);

        var dto = mapper.mapTilDto(YtelseType.OMSORGSPENGER, grunnlag, true);

        JsonObjectMapper.getMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, dto);

        assertThat(dto).isNotNull();
    }

    private YtelseBuilder lagYtelse() {
        Saksnummer sakId = new Saksnummer("1200094");
        YtelseBuilder ytelselseBuilder = YtelseBuilder.oppdatere(Optional.empty())
                .medKilde(Fagsystem.K9SAK)
                .medYtelseType(FagsakYtelseType.SYKEPENGER)
                .medSaksnummer(sakId);

        ytelselseBuilder.tilbakestillAnvisteYtelser();
        return ytelselseBuilder.medKilde(Fagsystem.INFOTRYGD)
            .medYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM_DATO, TOM_DATO))
            .medSaksnummer(sakId)
            .medYtelseGrunnlag(
                ytelselseBuilder.getGrunnlagBuilder()
                    .medOpprinneligIdentdato(OPPRINNELIG_IDENTDATO)
                    .medInntektsgrunnlagProsent(new BigDecimal(99.00))
                    .medDekningsgradProsent(new BigDecimal(98.00))
                    .medYtelseStørrelse(YtelseStørrelseBuilder.ny()
                        .medBeløp(new BigDecimal(100000.50))
                        .medVirksomhet(ORGNR)
                        .build())
                    .medVedtaksDagsats(new Beløp(557))
                    .build())
            .medYtelseAnvist(ytelselseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ANVIST_FOM, ANVIST_TOM))
                .medDagsats(new BigDecimal(500.00))
                .medUtbetalingsgradProsent(null)
                .build());
    }

}
