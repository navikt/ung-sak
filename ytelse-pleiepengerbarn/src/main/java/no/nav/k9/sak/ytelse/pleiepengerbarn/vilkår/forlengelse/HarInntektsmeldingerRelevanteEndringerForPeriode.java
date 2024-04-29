package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.HarEndretInntektsmeldingVurderer;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.InntektsmeldingerEndringsvurderer;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class HarInntektsmeldingerRelevanteEndringerForPeriode {

    private MottatteDokumentRepository mottatteDokumentRepository;

    private BehandlingRepository behandlingRepository;
    private Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForVilkårsrevurdering;

    private Instance<InntektsmeldingerEndringsvurderer> inntektsmeldingEndringVurderer;


    HarInntektsmeldingerRelevanteEndringerForPeriode() {
    }

    @Inject
    public HarInntektsmeldingerRelevanteEndringerForPeriode(BehandlingRepository behandlingRepository,
                                                            MottatteDokumentRepository mottatteDokumentRepository,
                                                            @Any Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForVilkårsrevurdering,
                                                            @Any Instance<InntektsmeldingerEndringsvurderer> endringsvurderere) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.inntektsmeldingEndringVurderer = endringsvurderere;
        this.inntektsmeldingRelevantForVilkårsrevurdering = inntektsmeldingRelevantForVilkårsrevurdering;
    }


    public Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringerForPeriode(Collection<Inntektsmelding> inntektsmeldinger,
                                                                                            BehandlingReferanse behandlingReferanse,
                                                                                            DatoIntervallEntitet periode,
                                                                                            VilkårType vilkårType) {

        var inntektsmeldingFilter = InntektsmeldingRelevantForVilkårsrevurdering.finnTjeneste(inntektsmeldingRelevantForVilkårsrevurdering, vilkårType, behandlingReferanse.getFagsakYtelseType());

        if (inntektsmeldingFilter.isEmpty()) {
            return Set.of();
        }

        var endringsvurderer = InntektsmeldingerEndringsvurderer.finnTjeneste(inntektsmeldingEndringVurderer, vilkårType, behandlingReferanse.getFagsakYtelseType());
        var filtrerOgFinnEndringer = new HarEndretInntektsmeldingVurderer(inntektsmeldingFilter.get()::begrensInntektsmeldinger, endringsvurderer);
        var mottatteInntektsmeldinger = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandlingReferanse.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();
        var originalBehandlingreferanse = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingReferanse.getOriginalBehandlingId().orElseThrow()));
        return filtrerOgFinnEndringer.finnInntektsmeldingerMedRelevanteEndringerForPerioden(
            behandlingReferanse,
            originalBehandlingreferanse,
            periode, inntektsmeldinger,
            mottatteInntektsmeldinger
        );
    }

}
