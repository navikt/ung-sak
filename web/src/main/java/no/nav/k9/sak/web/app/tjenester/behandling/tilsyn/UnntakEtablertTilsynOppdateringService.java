package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.*;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@Dependent
public class UnntakEtablertTilsynOppdateringService {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private BehandlingRepository behandlingRepository;

    public UnntakEtablertTilsynOppdateringService() {
        // CDI
    }

    @Inject
    public UnntakEtablertTilsynOppdateringService(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository, BehandlingRepository behandlingRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public OppdateringResultat oppdater(List<VurderingDto> vurderinger, Vurderingstype vurderingstype, Long behandlingId, AktørId søkersAktørId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return oppdater(vurderinger, vurderingstype, behandlingId, søkersAktørId, behandling.getFagsak().getPleietrengendeAktørId());
    }

    OppdateringResultat oppdater(List<VurderingDto> vurderinger, Vurderingstype vurderingstype, Long behandlingId, AktørId søkersAktørId, AktørId pleietrengende) {
        var eksisterendeGrunnlag = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(pleietrengende).orElseThrow();
        var unntakEtablertTilsyn = finnUnntakEtablertTilsyn(vurderingstype, eksisterendeGrunnlag);

        if (vurderinger != null) {
            for (VurderingDto vurdering : vurderinger) {
                unntakEtablertTilsyn = oppdater(unntakEtablertTilsyn, vurdering, behandlingId, søkersAktørId);
            }
        }

        var nyttUnntakEtablertTilsynForPleietrengende = switch(vurderingstype) {
            case BEREDSKAP ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getPleietrengendeAktørId(),
                    unntakEtablertTilsyn,
                    eksisterendeGrunnlag.getNattevåk()
                );
            case NATTEVÅK ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getPleietrengendeAktørId(),
                    eksisterendeGrunnlag.getBeredskap(),
                    unntakEtablertTilsyn
                );
        };

        unntakEtablertTilsynGrunnlagRepository.lagre(behandlingId, nyttUnntakEtablertTilsynForPleietrengende);

        return OppdateringResultat.utenOverhopp();
    }

    private UnntakEtablertTilsyn oppdater(UnntakEtablertTilsyn unntakEtablertTilsyn, VurderingDto vurdering, Long behandlingId, AktørId søkersAktørId) {
        return BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraAksjonspunkt(
            unntakEtablertTilsyn,
            LocalDate.now(),
            søkersAktørId,
            behandlingId,
            List.of(new Unntaksperiode(vurdering.getPeriode().getFom(), vurdering.getPeriode().getTom(), vurdering.getBegrunnelse(), vurdering.getResultat()))
        );
    }

    private UnntakEtablertTilsyn finnUnntakEtablertTilsyn(Vurderingstype vurderingstype, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        return switch (vurderingstype) {
            case BEREDSKAP -> unntakEtablertTilsynForPleietrengende.getBeredskap();
            case NATTEVÅK -> unntakEtablertTilsynForPleietrengende.getNattevåk();
        };
    }

}

enum Vurderingstype {
    BEREDSKAP,
    NATTEVÅK
}
