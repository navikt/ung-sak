package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.PermisjonBuilder;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class UtledPermisjonSomFørerTilAksjonspunktTest {

    private static final InternArbeidsforholdRef AREBIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private static final LocalDate DAGENS_DATO = LocalDate.now();

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder;
    private YrkesaktivitetBuilder yrkesaktivitetBuilder;
    private List<Yrkesaktivitet> yrkesaktiviteter;

    @Before
    public void oppsett(){
        Long behandlingId = 1L;
        AktørId aktørId = AktørId.dummy();
        InntektArbeidYtelseAggregatBuilder iayAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        aktørArbeidBuilder = iayAggregatBuilder.getAktørArbeidBuilder(aktørId);
        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(
            AREBIDSFORHOLD_ID, null, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter = new ArrayList<>();
    }

    @Test
    public void skal_ikke_filtrere_ut_permisjon_selv_med_flere_yrkesaktivteter_med_arbeidtype_fra_aareg(){

        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);

        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(
            InternArbeidsforholdRef.nyRef(), null, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        Permisjon permisjon_1 = byggPermisjon(DAGENS_DATO.minusYears(5), DAGENS_DATO.plusYears(1), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale_1 = byggAktivitetsAvtale(DAGENS_DATO.minusYears(5), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet_1 = byggYrkesAktivitet(permisjon_1, aktivitetsAvtale_1, "1", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet_1);

        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(
            InternArbeidsforholdRef.nyRef(), null, null), ArbeidType.MARITIMT_ARBEIDSFORHOLD);
        Permisjon permisjon_2 = byggPermisjon(DAGENS_DATO.minusYears(4), DAGENS_DATO.plusYears(1), PermisjonsbeskrivelseType.PERMITTERING, BigDecimal.valueOf(200));
        AktivitetsAvtaleBuilder aktivitetsAvtale_2 = byggAktivitetsAvtale(DAGENS_DATO.minusYears(4), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet_2 = byggYrkesAktivitet(permisjon_2, aktivitetsAvtale_2, "2", ArbeidType.MARITIMT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet_2);

        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(
            InternArbeidsforholdRef.nyRef(), null, null), ArbeidType.FORENKLET_OPPGJØRSORDNING);
        Permisjon permisjon_3 = byggPermisjon(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1), PermisjonsbeskrivelseType.VELFERDSPERMISJON, BigDecimal.valueOf(300));
        AktivitetsAvtaleBuilder aktivitetsAvtale_3 = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet_3 = byggYrkesAktivitet(permisjon_3, aktivitetsAvtale_3, "3", ArbeidType.FORENKLET_OPPGJØRSORDNING);
        yrkesaktiviteter.add(yrkesaktivitet_3);

        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);

        // Assert
        assertThat(permisjoner).hasSize(3);

    }

    @Test
    public void skal_filtrere_ut_permisjon_hvis_arbeidstype_ikke_er_fra_aareg(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(1), DAGENS_DATO.plusMonths(6), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.FRILANSER);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_filtrere_ut_permisjon_som_ikke_har_ansettelsesperioder_som_overlapper_stp(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(2), null, PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(5), DAGENS_DATO.minusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_filtrere_ut_permisjon_som_har_mindre_enn_100_prosentsats(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(1), DAGENS_DATO.plusMonths(6), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(99));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_filtrere_ut_permisjon_som_starter_etter_stp(){
        // Arrange
        LocalDate stp = DAGENS_DATO.minusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO, DAGENS_DATO.plusMonths(6), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_filtrere_ut_permisjon_som_slutter_før_stp(){
        // Arrange
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusMonths(6), DAGENS_DATO.minusDays(1), PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, DAGENS_DATO);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_ikke_filtrere_ut_permisjon_som_slutter_samtiding_som_stp(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(1), stp, PermisjonsbeskrivelseType.PERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(1);
    }

    @Test
    public void skal_filtrere_ut_permisjon_av_type_UTDANNINGSPERMISJON(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(1), DAGENS_DATO.plusMonths(6), PermisjonsbeskrivelseType.UTDANNINGSPERMISJON, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    @Test
    public void skal_filtrere_ut_permisjon_av_type_PERMISJON_MED_FORELDREPENGER(){
        // Arrange
        LocalDate stp = DAGENS_DATO.plusMonths(1);
        Permisjon permisjon = byggPermisjon(DAGENS_DATO.minusYears(1), DAGENS_DATO.plusMonths(6), PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER, BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder aktivitetsAvtale = byggAktivitetsAvtale(DAGENS_DATO.minusYears(3), DAGENS_DATO.plusYears(1));
        Yrkesaktivitet yrkesaktivitet = byggYrkesAktivitet(permisjon, aktivitetsAvtale, "123", ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktiviteter.add(yrkesaktivitet);
        // Act
        List<Permisjon> permisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, stp);
        // Assert
        assertThat(permisjoner).hasSize(0);
    }

    private Yrkesaktivitet byggYrkesAktivitet(Permisjon permisjon, AktivitetsAvtaleBuilder aktivitetsAvtale, String orgNr, ArbeidType arbeidType) {
        return yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
            .medArbeidsforholdId(null)
            .medArbeidType(arbeidType)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilPermisjon(permisjon)
            .build();
    }

    private AktivitetsAvtaleBuilder byggAktivitetsAvtale(LocalDate aaFom, LocalDate aaTom) {
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        return aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aaFom, aaTom));
    }

    private Permisjon byggPermisjon(LocalDate permisjonFom, LocalDate permisjonTom, PermisjonsbeskrivelseType permisjonType, BigDecimal permisjonProsent) {
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();
        return permisjonBuilder
            .medProsentsats(permisjonProsent)
            .medPeriode(permisjonFom, permisjonTom)
            .medPermisjonsbeskrivelseType(permisjonType)
            .build();
    }

}
