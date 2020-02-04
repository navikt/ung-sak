package no.nav.foreldrepenger.domene.abakus.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.InntektsKilde;
import no.nav.k9.kodeverk.iay.InntektspostType;
import no.nav.k9.kodeverk.iay.OffentligYtelseType;
import no.nav.k9.kodeverk.iay.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.iay.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.iay.TemaUnderkategori;

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
            .medYtelse(OffentligYtelseType.UDEFINERT);

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

        var dto = mapper.mapTilDto(grunnlag, true);

        IayGrunnlagJsonMapper.getMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, dto);

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
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT)
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
