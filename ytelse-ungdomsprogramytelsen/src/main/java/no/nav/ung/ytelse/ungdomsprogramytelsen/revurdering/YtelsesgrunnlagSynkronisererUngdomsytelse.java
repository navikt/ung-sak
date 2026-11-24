package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger.YtelsesgrunnlagSynkroniserer;

/**
 * Programperioden og sats og uttak avgjør ytelsesperioden i ungdomsprogramytelsen.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class YtelsesgrunnlagSynkronisererUngdomsytelse implements YtelsesgrunnlagSynkroniserer {

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    YtelsesgrunnlagSynkronisererUngdomsytelse() {
        // for CDI proxy
    }

    @Inject
    public YtelsesgrunnlagSynkronisererUngdomsytelse(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public void erstattYtelsesgrunnlagFraOrdinærBehandling(Long fraBehandlingId, Long tilKontrollbehandlingId) {
        ungdomsprogramPeriodeRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
        ungdomsytelseGrunnlagRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
    }
}
