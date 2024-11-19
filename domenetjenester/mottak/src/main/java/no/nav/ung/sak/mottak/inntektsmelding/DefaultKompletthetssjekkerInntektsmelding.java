package no.nav.ung.sak.mottak.inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.dokument.DokumentTypeId;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.ung.sak.kompletthet.ManglendeVedlegg;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef
public class DefaultKompletthetssjekkerInntektsmelding implements KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;

    DefaultKompletthetssjekkerInntektsmelding() {
        // CDI
    }

    @Inject
    public DefaultKompletthetssjekkerInntektsmelding(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste) {
        this.inntektsmeldingArkivTjeneste = inntektsmeldingArkivTjeneste;
    }

    /**
     * Henter alle påkrevde inntektsmeldinger fra aa-reg, og filtrerer ut alle
     * mottate.
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er mottatt
     */
    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref, LocalDate vurderingsdato) {
        return doUtledManglendeInntektsmeldinger(ref, true, vurderingsdato);
    }

    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, LocalDate vurderingsdato) {
        return doUtledManglendeInntektsmeldinger(ref, false, vurderingsdato);
    }

    private List<ManglendeVedlegg> doUtledManglendeInntektsmeldinger(BehandlingReferanse ref, boolean spørAAregDirekte, LocalDate vurderingsdato) {
        List<ManglendeVedlegg> manglendeVedlegg = (spørAAregDirekte ? inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, false, vurderingsdato)
            : inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, false, vurderingsdato))
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it))
            .collect(Collectors.toList());
        return manglendeVedlegg;
    }
}
