package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

public final class MapYrkesaktivitetTilOpptjeningsperiodeTjeneste {


    private MapYrkesaktivitetTilOpptjeningsperiodeTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
                                                                              Yrkesaktivitet registerAktivitet,
                                                                              InntektArbeidYtelseGrunnlag grunnlag,
                                                                              OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                              Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                              DatoIntervallEntitet opptjeningPeriode) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, registerAktivitet.getArbeidType());
        return new ArrayList<>(mapAktivitetsavtaler(behandlingReferanse, registerAktivitet, grunnlag,
            vurderForSaksbehandling, type, opptjeningPeriode));
    }

    private static OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
            .stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapAktivitetsavtaler(BehandlingReferanse behandlingReferanse,
                                                                                  Yrkesaktivitet registerAktivitet,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                                  OpptjeningAktivitetType type,
                                                                                  DatoIntervallEntitet opptjeningPeriode) {
        List<OpptjeningsperiodeForSaksbehandling> perioderForAktivitetsavtaler = new ArrayList<>();
        LocalDate skjæringstidspunkt = opptjeningPeriode.getTomDato().plusDays(1);
        for (AktivitetsAvtale avtale : gjeldendeAvtaler(grunnlag, skjæringstidspunkt, registerAktivitet)) {
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(type)
                .medPeriode(avtale.getPeriode())
                .medBegrunnelse(avtale.getBeskrivelse())
                .medStillingsandel(finnStillingsprosent(registerAktivitet, skjæringstidspunkt));
            settArbeidsgiverInformasjon(registerAktivitet, builder);
            var input = new VurderStatusInput(type, behandlingReferanse);
            input.setOpptjeningsperiode(opptjeningPeriode);
            input.setRegisterAktivitet(registerAktivitet);
            builder.medVurderingsStatus(vurderForSaksbehandling.vurderStatus(input));
            perioderForAktivitetsavtaler.add(builder.build());
        }
        return perioderForAktivitetsavtaler;
    }

    public static void settArbeidsgiverInformasjon(Yrkesaktivitet yrkesaktivitet, OpptjeningsperiodeForSaksbehandling.Builder builder) {
        Arbeidsgiver arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(yrkesaktivitet.getArbeidsforholdRef(), arbeidsgiver));
        }
        if (yrkesaktivitet.getNavnArbeidsgiverUtland() != null) {
            builder.medArbeidsgiverUtlandNavn(yrkesaktivitet.getNavnArbeidsgiverUtland());
        }
    }

    private static Stillingsprosent finnStillingsprosent(Yrkesaktivitet registerAktivitet, LocalDate skjæringstidspunkt) {
        final Stillingsprosent defaultStillingsprosent = new Stillingsprosent(0);
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(null, List.of(registerAktivitet));
            return filter.getAktivitetsAvtalerForArbeid()
                .stream()
                .filter(aa -> aa.getProsentsats() != null)
                .filter(aa -> aa.getPeriode().inkluderer(skjæringstidspunkt))
                .map(AktivitetsAvtale::getProsentsats)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Stillingsprosent::getVerdi))
                .orElse(defaultStillingsprosent);
        }
        return defaultStillingsprosent;
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
                                                                 LocalDate skjæringstidspunktForOpptjening,
                                                                 Yrkesaktivitet registerAktivitet) {
        if (registerAktivitet.erArbeidsforhold()) {
            return MapAnsettelsesPeriodeOgPermisjon.beregn(grunnlag, registerAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), registerAktivitet).før(skjæringstidspunktForOpptjening).getAktivitetsAvtalerForArbeid();
    }

}
