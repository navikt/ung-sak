package no.nav.ung.sak.mottak.inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.kompletthet.ManglendeVedlegg;

public interface KompletthetssjekkerInntektsmelding {

    /**
     * Utleder manglende inntektsmeldinger
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er mottatt
     */
    List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref, LocalDate vurderingsdato);

    /**
     * Henter alle påkrevde inntektsmeldinger fra grunnlaget, og filterer ut alle
     * motatte.
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er motatt
     */
    List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, LocalDate vurderingsdato);

    public static KompletthetssjekkerInntektsmelding finnKompletthetssjekkerInntektsmeldingFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(KompletthetssjekkerInntektsmelding.class, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException("Fant ikke " + KompletthetssjekkerInntektsmelding.class.getSimpleName() + " for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

}
