package no.nav.ung.sak.domene.vedtak.vedtattytelse;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Henter barnetillegget som er lagt til grunn i en behandling. Implementeres per ytelse,
 * siden satsene lagres i ytelsesspesifikke grunnlag.
 */
public interface BarnetilleggTidslinjeTjeneste {

    static BarnetilleggTidslinjeTjeneste finnTjeneste(Instance<BarnetilleggTidslinjeTjeneste> instanser, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(BarnetilleggTidslinjeTjeneste.class, instanser, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Fant ikke " + BarnetilleggTidslinjeTjeneste.class.getSimpleName() + " for ytelse " + ytelseType));
    }

    /**
     * @return barnetillegg per periode, tom tidslinje dersom satser ikke er beregnet for behandlingen
     */
    LocalDateTimeline<Barnetillegg> hentBarnetilleggTidslinje(Long behandlingId);

    record Barnetillegg(int antallBarn, int dagsatsBarnetillegg) {
    }
}
