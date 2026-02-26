package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.vedtak.resultat.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngDetaljertResultatForPeriodeUtleder implements DetaljertResultatForPeriodeUtleder {


    private static final Map<BehandlingÅrsakType, DetaljertResultatInfo> ÅRSAK_RESULTAT_INNVILGELSE_MAP = Map.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_DØDSFALL),
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_ØKT_SATS),
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_FØDSEL),
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_DELTAKER_DØDSFALL),
        BehandlingÅrsakType.RE_HENDELSE_FJERN_PERIODE_UNGDOMSPROGRAM, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_FJERNE_PERIODE)
    );

    @Override
    public Set<DetaljertResultatInfo> bestemDetaljertResultat(LocalDateInterval periode, SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat, TilkjentYtelseVerdi tilkjentYtelse) {

        if (!vilkårResultat.ikkeVurderteVilkår().isEmpty()) {
            return Set.of(DetaljertResultatInfo.of(DetaljertResultatType.IKKE_VURDERT));
        }

        var avslåtteVilkår = vilkårResultat.avslåtteVilkår();

        var relevanteÅrsaker = vilkårResultat.behandlingÅrsaker();

        var resultater = new HashSet<DetaljertResultatInfo>();

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            resultater.add(kontrollerInntektDetaljertResultat(tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)
            || vilkårResultat.manuellOpprettetBehandling() && relevanteÅrsaker.contains(BehandlingÅrsakType.RE_SATS_ENDRING)) {
            resultater.add(nyPeriodeDetaljertResultat(avslåtteVilkår, tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            resultater.add(endretStartdatoDetaljertResultat(avslåtteVilkår));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            resultater.add(endretSluttdatoDetaljertResultat(avslåtteVilkår));
        }

        relevanteÅrsaker.stream()
            .filter(ÅRSAK_RESULTAT_INNVILGELSE_MAP::containsKey)
            .map(årsak -> behandlingsårsakDetaljertResultat(årsak, avslåtteVilkår))
            .forEach(resultater::add);


        if (resultater.isEmpty()) {
            return håndterUkjenteTilfeller(tilkjentYtelse, avslåtteVilkår, relevanteÅrsaker)
                .map(Set::of)
                .orElseThrow(() -> new IllegalStateException(
                    "Klarte ikke å utlede resultattype for periode %s og vilkårsresultat og behandlingsårsaker %s og tilkjent ytelse %s"
                        .formatted(periode, vilkårResultat, tilkjentYtelse)));
        }


        return resultater;
    }



    private static DetaljertResultatInfo behandlingsårsakDetaljertResultat(BehandlingÅrsakType key, Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (avslåtteVilkår.isEmpty()) {
            return ÅRSAK_RESULTAT_INNVILGELSE_MAP.get(key);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslåtte inngangsvilkår, med behandlingsårsak %s".formatted(key));
    }

    private static Optional<DetaljertResultatInfo> håndterUkjenteTilfeller(TilkjentYtelseVerdi tilkjentYtelse, Set<DetaljertVilkårResultat> avslåtteVilkår, Set<BehandlingÅrsakType> relevanteÅrsaker) {
        if (!avslåtteVilkår.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslåtte inngangsvilkår ukjent årsak"));
        }

        if (tilkjentYtelse == null) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ANNET, "Innvilgede vilkår uten tilkjent ytelse"));
        }

        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Innvilgede vilkår 0 kr tilkjent ytelse"));
        }

        if (relevanteÅrsaker.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGET_UTEN_ÅRSAK, "Innvilget uten årsak"));
        }

        return Optional.empty();
    }

    private static DetaljertResultatInfo endretSluttdatoDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (harAvslåttVilkår(avslåtteVilkår, VilkårType.UNGDOMSPROGRAMVILKÅRET)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_SLUTTDATO, "Opphør av ungdomsprogramperiode");
        } else {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_SLUTTDATO, "Opphørsdato flyttet fremover");
        }
    }

    private static DetaljertResultatInfo endretStartdatoDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (harAvslåttVilkår(avslåtteVilkår, VilkårType.UNGDOMSPROGRAMVILKÅRET)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_STARTDATO, "Endring av startdato fremover");
        } else {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_STARTDATO, "Endring av startdato bakover");
        }
    }

    private static DetaljertResultatInfo nyPeriodeDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår, TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            if (!avslåtteVilkår.isEmpty()) {
                return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslått inngangsvilkår for ny periode");
            }

            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_UTBETALING);
    }

    private static DetaljertResultatInfo kontrollerInntektDetaljertResultat(TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            // Usikker om dette er mulig
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_UTEN_TILKJENT_YTELSE);
        }
        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.valueOf(100)) >= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
        }

        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) <= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
    }

    private static boolean harAvslåttVilkår(Set<DetaljertVilkårResultat> avslåtteVilkår, VilkårType vilkårType) {
        return avslåtteVilkår.stream().anyMatch
            (it -> it.vilkårType() == vilkårType);
    }

}
