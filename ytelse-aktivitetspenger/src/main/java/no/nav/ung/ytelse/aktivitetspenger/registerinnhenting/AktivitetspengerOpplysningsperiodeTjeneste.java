package no.nav.ung.ytelse.aktivitetspenger.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.registerinnhenting.OpplysningsperiodeTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@FagsakYtelseTypeRef(AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerOpplysningsperiodeTjeneste implements OpplysningsperiodeTjeneste {

    private BehandlingRepository behandlingRepository;

    AktivitetspengerOpplysningsperiodeTjeneste() {
    }

    @Inject
    public AktivitetspengerOpplysningsperiodeTjeneste(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato) {
        DatoIntervallEntitet fagsakperiode = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode();

        // 5 år tilbake pga medlemsakpsvilkår
        return new Periode(
            fagsakperiode.getFomDato().minusYears(5),
            fagsakperiode.getTomDato()
        );

    }

}
