package no.nav.k9.sak.ytelse.ung.registerdata;


import java.util.Collection;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriode;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.k9.sak.ytelse.ung.registerdata.ungdomsprogramregister.UngdomsprogramRegisterKlient;

@Dependent
public class UngdomsprogramTjeneste {
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public UngdomsprogramTjeneste(UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient, BehandlingRepository behandlingRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public UngdomsprogramTjeneste() {
    }

    public void innhentOpplysninger(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var registerOpplysninger = ungdomsprogramRegisterKlient.hentForAktørId(behandling.getFagsak().getAktørId().getAktørId());

        if (registerOpplysninger.opplysninger().isEmpty()) {
            throw new IllegalStateException("Fant ingen opplysninger om ungdomsprogrammet for aktør. ");
        }

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), mapPerioder(registerOpplysninger));
    }

    private static Collection<UngdomsprogramPeriode> mapPerioder(UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO dto) {
        return dto.opplysninger().stream().map(it -> new UngdomsprogramPeriode(it.fraOgMed(), it.tilOgMed())).toList();
    }

}
