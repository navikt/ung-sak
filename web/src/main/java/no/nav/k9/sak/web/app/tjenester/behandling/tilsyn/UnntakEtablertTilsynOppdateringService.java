package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.Vurdering;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.BeredskapOgNattevåkOversetter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.Unntaksperiode;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@Dependent
public class UnntakEtablertTilsynOppdateringService {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private UnntakEtablertTilsynRepository unntakEtablertTilsynRepository;

    public UnntakEtablertTilsynOppdateringService() {
        // CDI
    }

    @Inject
    public UnntakEtablertTilsynOppdateringService(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                                  UnntakEtablertTilsynRepository unntakEtablertTilsynRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.unntakEtablertTilsynRepository = unntakEtablertTilsynRepository;
    }

    public OppdateringResultat oppdater(Vurdering dto, Long behandlingId, AktørId søkersAktørId) {
        var unntakEtablertTilsynGrunnlag = unntakEtablertTilsynGrunnlagRepository.hent(behandlingId);

        var perioder = dto.getPerioder().stream().map(periode -> new Unntaksperiode(periode.getFom(), periode.getTom(), dto.getVurderingstekst())).toList();

        var beredskap = BeredskapOgNattevåkOversetter.tilUnntakEtablertTilsynForPleietrengende(
            unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap(),
            LocalDate.now(),
            søkersAktørId,
            perioder,
            List.of());

        unntakEtablertTilsynRepository.lagre(beredskap);
        unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().medBeredskap(beredskap);
        unntakEtablertTilsynGrunnlagRepository.lagre(unntakEtablertTilsynGrunnlag);

        return OppdateringResultat.utenOveropp();
    }

}
