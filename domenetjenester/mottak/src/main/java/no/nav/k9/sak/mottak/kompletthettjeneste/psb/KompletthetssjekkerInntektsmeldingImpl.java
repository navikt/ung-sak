package no.nav.k9.sak.mottak.kompletthettjeneste.psb;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef
public class KompletthetssjekkerInntektsmeldingImpl implements KompletthetssjekkerInntektsmelding {

    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;

    KompletthetssjekkerInntektsmeldingImpl() {
        // CDI
    }

    @Inject
    public KompletthetssjekkerInntektsmeldingImpl(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste) {
        this.inntektsmeldingArkivTjeneste = inntektsmeldingArkivTjeneste;
    }

    /**
     * Henter alle påkrevde inntektsmeldinger fra aa-reg, og filtrerer ut alle
     * mottate.
     *
     * @return Manglende påkrevde inntektsmeldinger som ennå ikke er mottatt
     */
    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldinger(BehandlingReferanse ref) {
        return doUtledManglendeInntektsmeldinger(ref, true);
    }

    @Override
    public List<ManglendeVedlegg> utledManglendeInntektsmeldingerFraGrunnlag(BehandlingReferanse ref) {
        return doUtledManglendeInntektsmeldinger(ref, false);
    }

    private List<ManglendeVedlegg> doUtledManglendeInntektsmeldinger(BehandlingReferanse ref, boolean brukArkiv) {
        List<ManglendeVedlegg> manglendeVedlegg = (brukArkiv ? inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(ref, false)
            : inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, false))
            .keySet()
            .stream()
            .map(it -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, it.getIdentifikator()))
            .collect(Collectors.toList());
        return manglendeVedlegg;
    }
}
