package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNødvendighetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNødvendighetOppdaterer implements AksjonspunktOppdaterer<VurderNødvendighetDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public VurderNødvendighetOppdaterer() {
    }

    @Inject
    public VurderNødvendighetOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderNødvendighetDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertOpplæring> vurdertOpplæring = new ArrayList<>();

        Optional<VurdertOpplæringHolder> aktivVurdertOpplæringHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertOpplæringHolder);

        if (aktivVurdertOpplæringHolder.isPresent()) {
            List<VurdertOpplæring> aktiveVurdertInstitusjoner = aktivVurdertOpplæringHolder.get().getVurdertOpplæring().stream()
                .filter(aktivVurdertOpplæring -> !aktivVurdertOpplæring.getJournalpostId().equals(dto.getJournalpostId().getJournalpostId()))
                .toList();
            vurdertOpplæring.addAll(aktiveVurdertInstitusjoner);
        }

        Optional<VurdertOpplæring> eksisterendeVurdertOpplæring = aktivVurdertOpplæringHolder.flatMap(holder -> holder.finnVurderingForJournalpostId(dto.getJournalpostId().getJournalpostId()));

        VurdertOpplæring nyVurdertOpplæring = mapDtoTilVurdertOpplæring(dto, eksisterendeVurdertOpplæring.map(VurdertOpplæring::getPerioder).orElse(List.of()));
        vurdertOpplæring.add(nyVurdertOpplæring);

        VurdertOpplæringHolder nyHolder = new VurdertOpplæringHolder(vurdertOpplæring);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private VurdertOpplæring mapDtoTilVurdertOpplæring(VurderNødvendighetDto dto, List<VurdertOpplæringPeriode> perioder) {
        return new VurdertOpplæring(dto.getJournalpostId().getJournalpostId(), perioder, dto.isNødvendigOpplæring(), dto.getBegrunnelse());
    }
}
