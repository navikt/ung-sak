package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderInstitusjonDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderInstitusjonDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderInstitusjonOppdaterer implements AksjonspunktOppdaterer<VurderInstitusjonDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public VurderInstitusjonOppdaterer() {
    }

    @Inject
    public VurderInstitusjonOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderInstitusjonDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertInstitusjon> vurderteInstitusjoner = new ArrayList<>();
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon(dto.getJournalpostId().getJournalpostId(), dto.isGodkjent(), dto.getBegrunnelse(), getCurrentUserId(), LocalDateTime.now());
        vurderteInstitusjoner.add(vurdertInstitusjon);

        var aktivVurdertInstitusjonHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertInstitusjonHolder);

        if (aktivVurdertInstitusjonHolder.isPresent()) {
            List<VurdertInstitusjon> aktiveVurdertInstitusjoner = aktivVurdertInstitusjonHolder.get().getVurdertInstitusjon().stream()
                .filter(eksisterendeVurdertInstitusjon -> !eksisterendeVurdertInstitusjon.getJournalpostId().equals(vurdertInstitusjon.getJournalpostId()))
                .toList();
            vurderteInstitusjoner.addAll(aktiveVurdertInstitusjoner);
        }

        VurdertInstitusjonHolder nyHolder = new VurdertInstitusjonHolder(vurderteInstitusjoner);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private static String getCurrentUserId() {
        String brukerident = SubjectHandler.getSubjectHandler().getUid();
        return brukerident != null ? brukerident : "VL";
    }
}
