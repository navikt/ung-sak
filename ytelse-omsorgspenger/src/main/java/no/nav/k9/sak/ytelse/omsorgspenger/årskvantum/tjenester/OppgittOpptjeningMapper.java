package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;


@Dependent
class OppgittOpptjeningMapper {

    /**
     * Sammenstiller oppgitt opptjening per inntekttype fra alle oppgitte inntjeninger
     * {@link OppgittEgenNæring} {@link OppgittFrilans} {@link OppgittArbeidsforhold} {@link OppgittAnnenAktivitet}
     */
    static OppgittOpptjening sammenstillOppgittOpptjening(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger) {
        // TODO: Lage buildermetoder med enklere api for å sammenstille oppgitte opptjeninger
        var builder = OppgittOpptjeningBuilder.ny();

        var oppgittOpptjeningSN = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getEgenNæring().isEmpty())
            .findFirst();
        oppgittOpptjeningSN.ifPresent(opptj -> {
            var buildersSN = opptj.getEgenNæring().stream()
                .map(egenNæring -> OppgittOpptjeningBuilder.EgenNæringBuilder.fraEksisterende(egenNæring))
                .collect(Collectors.toList());
            builder.leggTilEgneNæringer(buildersSN);
        });

        var oppgittOpptjeningFL = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> opptj.getFrilans().isPresent())
            .findFirst();
        oppgittOpptjeningFL.ifPresent(opptj -> {
            var builderFL = opptj.getFrilans().map(fl -> OppgittOpptjeningBuilder.OppgittFrilansBuilder.fraEksisterende(fl));
            builderFL.ifPresent(b -> builder.leggTilFrilansOpplysninger(b.build()));
        });

        var oppgittOpptjeningAT = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getOppgittArbeidsforhold().isEmpty())
            .findFirst();
        oppgittOpptjeningAT.ifPresent(opptj -> {
            var buildersAT = opptj.getOppgittArbeidsforhold().stream()
                .map(af -> OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.fraEksisterende(af))
                .collect(Collectors.toList());
            builder.leggTilOppgittArbeidsforhold(buildersAT);
        });

        var oppgittOpptjeningAnnet = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getAnnenAktivitet().isEmpty())
            .findFirst();
        oppgittOpptjeningAnnet.ifPresent(opptj -> {
            var andreAktiviteter = opptj.getAnnenAktivitet();
            andreAktiviteter.forEach(aa -> builder.leggTilAnnenAktivitet(aa));
        });

        return builder.build();
    }
}
