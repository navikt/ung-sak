package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.iay.RelatertYtelseTilstand;

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
