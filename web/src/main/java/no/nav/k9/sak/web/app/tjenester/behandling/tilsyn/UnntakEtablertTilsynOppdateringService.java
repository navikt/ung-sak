package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.Vurdering;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingBeredskapDto;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingNattevåkDto;
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

    public OppdateringResultat oppdater(Vurdering dto, Long behandlingId, AktørId søkersAktørId) {
        var eksisterendeGrunnlag = unntakEtablertTilsynGrunnlagRepository.hent(behandlingId);
        var vurderingstype = finnVurderingstype(dto);
        var perioder = dto.getPerioder().stream().map(periode -> new Unntaksperiode(periode.getFom(), periode.getTom(), dto.getVurderingstekst())).toList();

        var nyttUnntakEtablertTilsyn = BeredskapOgNattevåkOversetter.tilUnntakEtablertTilsynForPleietrengende(
            finnUnntakEtablertTilsyn(vurderingstype, eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende()),
            LocalDate.now(),
            søkersAktørId,
            behandlingId,
            dto.getVurderingstekst(),
            perioder,
            List.of());

        eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().medBeredskap(nyttUnntakEtablertTilsyn);

        var nyttUnntakEtablertTilsynForPleietrengende = switch(vurderingstype) {
            case BEREDSKAP ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getPleietrengendeAktørId(),
                    nyttUnntakEtablertTilsyn,
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk()
                );
            case NATTEVÅK ->
                new UnntakEtablertTilsynForPleietrengende(
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getPleietrengendeAktørId(),
                    eksisterendeGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap(),
                    nyttUnntakEtablertTilsyn
                );
        };

        unntakEtablertTilsynGrunnlagRepository.lagre(behandlingId, nyttUnntakEtablertTilsynForPleietrengende);

        return OppdateringResultat.utenOveropp();
    }

    private Vurderingstype finnVurderingstype(Vurdering vurdering) {
        if (vurdering instanceof VurderingBeredskapDto) {
            return Vurderingstype.BEREDSKAP;
        } else if (vurdering instanceof VurderingNattevåkDto) {
            return Vurderingstype.NATTEVÅK;
        }
        throw new IllegalArgumentException("Ugyldig subklasse av Vurdering");
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
