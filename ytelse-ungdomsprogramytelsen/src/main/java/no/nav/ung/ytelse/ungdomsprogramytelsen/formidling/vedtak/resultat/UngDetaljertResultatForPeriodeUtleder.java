package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.vedtak.resultat.*;

import java.util.HashSet;
import java.util.Map;
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
            resultater.add(DetaljertResultatForPeriodeUtleder.kontrollerInntektDetaljertResultat(tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)
            || vilkårResultat.manuellOpprettetBehandling() && relevanteÅrsaker.contains(BehandlingÅrsakType.RE_SATS_ENDRING)) {
            resultater.add(DetaljertResultatForPeriodeUtleder.nyPeriodeDetaljertResultat(avslåtteVilkår, tilkjentYtelse));
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
            return DetaljertResultatForPeriodeUtleder.håndterUkjenteTilfeller(tilkjentYtelse, avslåtteVilkår, relevanteÅrsaker)
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

    private static boolean harAvslåttVilkår(Set<DetaljertVilkårResultat> avslåtteVilkår, VilkårType vilkårType) {
        return avslåtteVilkår.stream().anyMatch
            (it -> it.vilkårType() == vilkårType);
    }

}
