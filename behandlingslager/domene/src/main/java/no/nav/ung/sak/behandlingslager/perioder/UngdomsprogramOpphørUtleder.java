package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

import java.util.Set;

/**
 * Leser periodegrunnlaget for ungdomsprogrammet og svarer på om programperioden er lukket/åpen for en gitt behandling.
 */
public final class UngdomsprogramOpphørUtleder {

    private UngdomsprogramOpphørUtleder() {
    }

    public static boolean harLukketProgramperiode(Long behandlingId, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return harLukketSluttdato(hentPerioder(behandlingId, ungdomsprogramPeriodeRepository));
    }

    /**
     * Merk at dette <em>ikke</em> er negasjonen av {@link #opphørAvUngdomsprogrammetVarInkludertIVedtaket}: når det
     * ikke finnes en originalbehandling er begge {@code false}.
     */
    public static boolean forrigeBehandlingVarLøpende(Behandling behandling, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return behandling.getOriginalBehandlingId()
            .map(id -> hentPerioder(id, ungdomsprogramPeriodeRepository))
            .map(UngdomsprogramOpphørUtleder::harÅpenSluttdato)
            .orElse(false);
    }

    /**
     * @return {@code true} dersom forrige vedtak hadde en lukket sluttdato — dvs. et reelt, iverksatt opphør som nå
     * kan oppheves. {@code false} dersom det ikke finnes en originalbehandling, eller den fortsatt var løpende.
     */
    public static boolean opphørAvUngdomsprogrammetVarInkludertIVedtaket(Behandling behandling, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return behandling.getOriginalBehandlingId()
            .map(id -> hentPerioder(id, ungdomsprogramPeriodeRepository))
            .map(UngdomsprogramOpphørUtleder::harLukketSluttdato)
            .orElse(false);
    }

    private static boolean harLukketSluttdato(Set<UngdomsprogramPeriode> perioder) {
        return !perioder.isEmpty() && perioder.stream().noneMatch(UngdomsprogramOpphørUtleder::erÅpen);
    }

    private static boolean harÅpenSluttdato(Set<UngdomsprogramPeriode> perioder) {
        return perioder.stream().anyMatch(UngdomsprogramOpphørUtleder::erÅpen);
    }

    private static boolean erÅpen(UngdomsprogramPeriode periode) {
        return Tid.TIDENES_ENDE.equals(periode.getPeriode().getTomDato());
    }

    private static Set<UngdomsprogramPeriode> hentPerioder(Long behandlingId, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder())
            .orElse(Set.of());
    }

}



