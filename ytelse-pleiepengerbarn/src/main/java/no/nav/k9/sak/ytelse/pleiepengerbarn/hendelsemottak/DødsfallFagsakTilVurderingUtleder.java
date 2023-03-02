package no.nav.k9.sak.ytelse.pleiepengerbarn.hendelsemottak;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.k9.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class DødsfallFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(DødsfallFagsakTilVurderingUtleder.class);
    public static final Set<FagsakYtelseType> RELEVANTE_YTELSER = Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    public DødsfallFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public DødsfallFagsakTilVurderingUtleder(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(AktørId aktørId, Hendelse hendelse) {
        List<AktørId> dødsfallAktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate dødsdato = hendelse.getHendelsePeriode().getFom();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (FagsakYtelseType fagsakYtelseType : RELEVANTE_YTELSER) {
            for (AktørId aktør : dødsfallAktører) {
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, aktør, null, null, dødsdato, null)) {
                    if (!hendelseHåndtertTidligere(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER)) {
                        fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
                    }
                }
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, null, aktør, null, dødsdato, null)) {
                    if (!hendelseHåndtertTidligere(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN)) {
                        fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
                    }
                }
            }
        }


        return fagsaker;
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     */
    private boolean hendelseHåndtertTidligere(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        for (Behandling behandling : behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer())) {
            if (behandling.getBehandlingÅrsaker().stream().anyMatch(årsak -> årsak.getBehandlingÅrsakType().equals(behandlingÅrsakType))) {
                logger.info("Dødsfallhendelse for sak {} var allerede håndtert i behandling {}", fagsak.getSaksnummer(), behandling.getUuid());
                return true;
            }
        }
        return false;
    }
}
