package no.nav.ung.sak.domene.registerinnhenting.impl;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.registerinnhenting.OpplysningsperiodeTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@ApplicationScoped
public class UngdomsytelseOpplysningsperiodeTjeneste implements OpplysningsperiodeTjeneste {

    private BehandlingRepository behandlingRepository;
    private final Period periodeEtter = Period.parse("P3M");
    private final Period periodeFør = Period.parse("P12M");

    UngdomsytelseOpplysningsperiodeTjeneste() {
    }

    @Inject
    public UngdomsytelseOpplysningsperiodeTjeneste(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato) {

        DatoIntervallEntitet fagsakperiode = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode();

        return new Periode(
            fagsakperiode.getFomDato().minus(periodeFør),
            fagsakperiode.getTomDato().plus(periodeEtter)
        );

    }

    @Override
    public Periode utledOpplysningsperiodeSkattegrunnlag(Long behandlingId) {
        throw new IllegalStateException("Ikke implementert enda");
    }
}
