package no.nav.ung.sak.domene.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.ung.sak.domene.iay.modell.Permisjon;
import no.nav.ung.sak.domene.iay.modell.PermisjonBuilder;
import no.nav.ung.sak.domene.iay.modell.VersjonType;
import no.nav.ung.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.ung.sak.domene.iay.modell.YtelseBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.Saksnummer;

public class YtelseTestHelper {

    public static InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder leggTilYtelse(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder,
                                                                                LocalDate fom, LocalDate tom,
                                                                                RelatertYtelseTilstand relatertYtelseTilstand, String saksnummer, FagsakYtelseType ytelseType) {


        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
                .medKilde(Fagsystem.INFOTRYGD)
                .medYtelseType(ytelseType)
                .medSaksnummer(new Saksnummer(saksnummer));
        ytelseBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        ytelseBuilder.medStatus(relatertYtelseTilstand);
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        return aktørYtelseBuilder;
    }

    public static InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
                                                                                                         DatoIntervallEntitet periode, ArbeidType type,
                                                                                                         BigDecimal prosentsats, Arbeidsgiver arbeidsgiver,
                                                                                                         LocalDate sisteLønnsendringsdato,
                                                                                                         VersjonType versjonType) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), versjonType);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null), type);

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, false);
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medProsentsats(prosentsats)
            .medSisteLønnsendringsdato(sisteLønnsendringsdato)
            .medBeskrivelse("Ser greit ut");
        final AktivitetsAvtaleBuilder ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, true);

        Permisjon permisjon = permisjonBuilder
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
            .medPeriode(periode.getFomDato(), periode.getTomDato())
            .medProsentsats(BigDecimal.valueOf(100))
            .build();

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .leggTilPermisjon(permisjon)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesPeriode);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }



}
