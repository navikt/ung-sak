package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;

class OppgittOpptjeningMapper {
    static OppgittOpptjening sammenstillOppgittOpptjening(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger) {
        var builder = OppgittOpptjeningBuilder.ny();

        leggTilEgneNæringer(overlappendeOppgitteOpptjeninger, builder);
        leggTilFrilans(overlappendeOppgitteOpptjeninger, builder);
        leggTilArbeidsforhold(overlappendeOppgitteOpptjeninger, builder);
        leggTilAndreAktiviteter(overlappendeOppgitteOpptjeninger, builder);

        return builder.build();
    }

    private static void leggTilEgneNæringer(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger, OppgittOpptjeningBuilder builder) {
        var oppgittOpptjeningSN = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getEgenNæring().isEmpty())
            .findFirst();
        oppgittOpptjeningSN.ifPresent(opptj -> {
            var buildersSN = opptj.getEgenNæring().stream()
                .map(egenNæring -> OppgittOpptjeningBuilder.EgenNæringBuilder.fraEksisterende(egenNæring))
                .collect(Collectors.toList());
            builder.leggTilEgneNæringer(buildersSN);
        });
    }

    private static void leggTilFrilans(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger, OppgittOpptjeningBuilder builder) {
        var oppgittOpptjeningFL = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> opptj.getFrilans().isPresent())
            .findFirst();
        oppgittOpptjeningFL.ifPresent(opptj -> {
            var builderFL = opptj.getFrilans().map(fl -> OppgittOpptjeningBuilder.OppgittFrilansBuilder.fraEksisterende(fl));
            builderFL.ifPresent(b -> builder.leggTilFrilansOpplysninger(b.build()));
        });
    }

    private static void leggTilArbeidsforhold(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger, OppgittOpptjeningBuilder builder) {
        var oppgittOpptjeningAT = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getOppgittArbeidsforhold().isEmpty())
            .findFirst();
        oppgittOpptjeningAT.ifPresent(opptj -> {
            var buildersAT = opptj.getOppgittArbeidsforhold().stream()
                .map(af -> OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.fraEksisterende(af))
                .collect(Collectors.toList());
            builder.leggTilOppgittArbeidsforhold(buildersAT);
        });
    }

    private static void leggTilAndreAktiviteter(List<OppgittOpptjening> overlappendeOppgitteOpptjeninger, OppgittOpptjeningBuilder builder) {
        var oppgittOpptjeningAnnet = overlappendeOppgitteOpptjeninger.stream()
            .filter(opptj -> !opptj.getAnnenAktivitet().isEmpty())
            .findFirst();
        oppgittOpptjeningAnnet.ifPresent(opptj -> {
            var andreAktiviteter = opptj.getAnnenAktivitet();
            andreAktiviteter.forEach(aa -> builder.leggTilAnnenAktivitet(aa));
        });
    }
}
