package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class EndringFjerneProgramPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    @Inject
    public EndringFjerneProgramPeriodeStrategy() {
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (resultatTidslinje.harÅrsak(BehandlingÅrsakType.RE_HENDELSE_FJERN_PERIODE_UNGDOMSPROGRAM)) {
            return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT, "Sender ikke brev ved fjerning av programperiode foreløpig"));
        }
        return List.of();
    }

}
