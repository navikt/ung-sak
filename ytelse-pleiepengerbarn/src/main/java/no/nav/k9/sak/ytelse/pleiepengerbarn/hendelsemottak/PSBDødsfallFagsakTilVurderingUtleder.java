package no.nav.k9.sak.ytelse.pleiepengerbarn.hendelsemottak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.k9.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class PSBDødsfallFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    public static final Set<FagsakYtelseType> RELEVANTE_YTELSER = Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
    private FagsakRepository fagsakRepository;

    public PSBDødsfallFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PSBDødsfallFagsakTilVurderingUtleder(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(AktørId aktørId, Hendelse hendelse) {
        List<AktørId> dødsfallAktører = hendelse.getHendelseInfo().getAktørIder();
        var periode = hendelse.getHendelsePeriode();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        for (FagsakYtelseType fagsakYtelseType : RELEVANTE_YTELSER) {
            for (AktørId aktør : dødsfallAktører) {
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, aktør, null, null, periode.getFom(), periode.getTom())) {
                    fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
                }
                for (Fagsak fagsak : fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, null, aktør, null, periode.getFom(), periode.getTom())) {
                    fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
                }
            }
        }

        return fagsaker;
    }
}
