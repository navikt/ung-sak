package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Representerer et snapshot av periodedata (fom/tom + referanse) uavhengig av {@link UngdomsprogramPeriodeGrunnlag}.
 * Brukes i sammenligningslogikken for å finne endringer i start- eller sluttdato.
 * Kan være konstruert fra et reelt grunnlag eller syntetisk fra oppgitt startdato i søknaden.
 */
record PeriodeSnapshot(Optional<LocalDate> fomDato, Optional<LocalDate> tomDato, UUID grunnlagsreferanse) {

    static PeriodeSnapshot fraGrunnlag(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var periode = grunnlag.hentForEksaktEnPeriodeDersomFinnes();
        return new PeriodeSnapshot(
            periode.map(p -> p.getFomDato()),
            periode.map(p -> p.getTomDato()),
            grunnlag.getGrunnlagsreferanse()
        );
    }

    /**
     * Syntetisk snapshot basert på oppgitt startdato fra søknaden.
     * Brukes for å håndtere caset der perioden endres mellom søknadstidspunkt og innhenting av periodeopplysninger,
     * slik at startdatoen kan sammenlignes mot hva bruker faktisk søkte på.
     */
    static PeriodeSnapshot fraOppgittStartdato(LocalDate oppgittStartdato) {
        return new PeriodeSnapshot(Optional.ofNullable(oppgittStartdato), Optional.empty(), null);
    }

}
