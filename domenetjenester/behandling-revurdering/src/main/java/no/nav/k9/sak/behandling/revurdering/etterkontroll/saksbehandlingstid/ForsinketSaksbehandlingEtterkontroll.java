package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;


import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRef;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste.KontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.MottakerDto;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@EtterkontrollRef(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
@ApplicationScoped
public class ForsinketSaksbehandlingEtterkontroll implements KontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingEtterkontroll.class);

    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public ForsinketSaksbehandlingEtterkontroll(
        DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
        BehandlingRepository behandlingRepository) {

        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    ForsinketSaksbehandlingEtterkontroll() {
    }

    @Override
    public boolean utfør(Etterkontroll etterkontroll) {
        Objects.requireNonNull(etterkontroll.getBehandlingId(), "Utvikler-feil: BehandlingId må være satt");

        var behandling = behandlingRepository.hentBehandling(etterkontroll.getBehandlingId());
        if (!behandling.erAvsluttet()) {
            log.info("Behandling er ikke ferdigstilt innen fristen. Bestiller brev om forlenget saksbehandling.");
            dokumentBestillerApplikasjonTjeneste.bestillDokument(new BestillBrevDto(
                behandling.getId(), DokumentMalType.FORLENGET_DOK,
                new MottakerDto(behandling.getAktørId().getId(), IdType.AKTØRID.toString())
            ), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else {
            log.info("Behandling ferdigstilt innen fristen. Bestiller ikke brev.");
        }

        return true;
    }
}
