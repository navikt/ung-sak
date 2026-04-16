package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

/**
 * Representerer et snapshot av periodedata (fom/tom + referanse) uavhengig av {@link UngdomsprogramPeriodeGrunnlag}.
 * Brukes i sammenligningslogikken for å finne endringer i start- eller sluttdato.
 * Kan være konstruert fra et reelt grunnlag eller syntetisk fra oppgitt startdato i søknaden.
 */
record PeriodeSnapshot(Optional<LocalDate> fomDato, Optional<LocalDate> tomDato, String beskrivelse) {

    static PeriodeSnapshot fraGrunnlag(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var periode = grunnlag.hentForEksaktEnPeriodeDersomFinnes();
        return new PeriodeSnapshot(
            periode.map(DatoIntervallEntitet::getFomDato),
            periode.map(DatoIntervallEntitet::getTomDato).filter(d -> !d.equals(TIDENES_ENDE)),
            "UngdomsprogramPeriodeGrunnlag-" + grunnlag.getGrunnlagsreferanse()
        );
    }


    /**
     * Syntetisk snapshot basert på oppgitt startdato fra søknaden.
     * Brukes for å håndtere caset der perioden endres mellom søknadstidspunkt og innhenting av periodeopplysninger,
     * slik at startdatoen kan sammenlignes mot hva bruker faktisk søkte på.
     */
    public static PeriodeSnapshot fraOppgittStartdato(UngdomsytelseSøktStartdato oppgittStartdato) {
        return new PeriodeSnapshot(Optional.of(oppgittStartdato.getStartdato()), Optional.empty(), "UngdomsytelseSøktStartdato-JP" + oppgittStartdato.getJournalpostId().getVerdi());


    }
}
