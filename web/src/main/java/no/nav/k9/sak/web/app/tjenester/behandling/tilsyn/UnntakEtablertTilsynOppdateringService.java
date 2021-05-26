package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.*;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@Dependent
public class UnntakEtablertTilsynOppdateringService {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;

    public UnntakEtablertTilsynOppdateringService() {
        // CDI
    }

    @Inject
    public UnntakEtablertTilsynOppdateringService(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
    }

    public OppdateringResultat oppdater(List<VurderingDto> vurderinger, Vurderingstype vurderingstype, Long behandlingId, AktørId søkersAktørId) {
        var eksisterendeGrunnlag = unntakEtablertTilsynGrunnlagRepository.hent(behandlingId);
        var unntakEtablertTilsyn = finnUnntakEtablertTilsyn(vurderingstype, eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende());

        if (vurderinger != null) {
            for (VurderingDto vurdering : vurderinger) {
                unntakEtablertTilsyn = oppdater(unntakEtablertTilsyn, vurdering, behandlingId, søkersAktørId);
            }
        }

        var nyttUnntakEtablertTilsynForPleietrengende = switch(vurderingstype) {
            case BEREDSKAP ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getPleietrengendeAktørId(),
                    unntakEtablertTilsyn,
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()
                );
            case NATTEVÅK ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getPleietrengendeAktørId(),
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap(),
                    unntakEtablertTilsyn
                );
        };

        unntakEtablertTilsynGrunnlagRepository.lagre(behandlingId, nyttUnntakEtablertTilsynForPleietrengende);

        return OppdateringResultat.utenOveropp();
    }

    private UnntakEtablertTilsyn oppdater(UnntakEtablertTilsyn unntakEtablertTilsyn, VurderingDto vurdering, Long behandlingId, AktørId søkersAktørId) {
        return BeredskapOgNattevåkOppdaterer.tilUnntakEtablertTilsynForPleietrengende(
            unntakEtablertTilsyn,
            LocalDate.now(),
            søkersAktørId,
            behandlingId,
            List.of(new Unntaksperiode(vurdering.getPeriode().getFom(), vurdering.getPeriode().getTom(), vurdering.getBegrunnelse(), vurdering.getResultat())),
            List.of(),
            false
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
