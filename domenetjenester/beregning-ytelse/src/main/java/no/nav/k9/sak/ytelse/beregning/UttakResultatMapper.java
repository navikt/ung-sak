package no.nav.k9.sak.ytelse.beregning;

import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;

@FunctionalInterface
public interface UttakResultatMapper {

    /**
     * Henter uttaksresultat fra repository og mapper til en felles regelmodell for
     * uttaksresultat ved hjelp av en mapper. Repository og mapper brukt avhenger
     * av fagsak-typen (foreldrepenger eller svangerskapspenger).
     *
     * @param input UttakInput
     * @return Regelmodell for uttakResultat
     */

    UttakResultat hentOgMapUttakResultat(UttakResultatInput input);
}
