package no.nav.ung.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;


import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.EtterkontrollRef;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.tjeneste.KontrollTjeneste;
import no.nav.ung.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@EtterkontrollRef(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
@ApplicationScoped
public class ForsinketSaksbehandlingEtterkontrollTjeneste implements KontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ForsinketSaksbehandlingEtterkontrollTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private Instance<SaksbehandlingsfristUtleder> fristUtledere;

    @Inject
    public ForsinketSaksbehandlingEtterkontrollTjeneste(
        BehandlingRepository behandlingRepository,
        @Any Instance<SaksbehandlingsfristUtleder> fristUtledere) {

        this.behandlingRepository = behandlingRepository;
        this.fristUtledere = fristUtledere;
    }

    ForsinketSaksbehandlingEtterkontrollTjeneste() {
    }

    @Override
    public boolean utfør(Etterkontroll etterkontroll) {
        Objects.requireNonNull(etterkontroll.getBehandlingId(), "Utvikler-feil: BehandlingId må være satt");

        var behandling = behandlingRepository.hentBehandling(etterkontroll.getBehandlingId());

        if (!behandling.erAvsluttet() && harFortsattFrist(behandling)) {
            log.info("Behandling er ikke ferdigstilt innen fristen. Bestiller brev om forlenget saksbehandling.");
            // TODO: Bestill brev
//            dokumentBestillerApplikasjonTjeneste.bestillDokument(new BestillBrevDto(
//                behandling.getId(), DokumentMalType.FORLENGET_DOK,
//                new MottakerDto(behandling.getAktørId().getId(), IdType.AKTØRID.toString()),
//                lagUnikDokumentbestillingId(etterkontroll, behandling.getUuid(), DokumentMalType.FORLENGET_DOK)
//            ), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else {
            log.info("Behandling ferdigstilt innen fristen eller har ikke lenger frist. Bestiller ikke brev.");
        }

        return true;
    }

    private static String lagUnikDokumentbestillingId(Etterkontroll etterkontroll, UUID uuid, DokumentMalType mal) {
        return UUID.nameUUIDFromBytes(String.format(
            "%d%s%s", etterkontroll.getId(), uuid.toString(), mal.getKode()
        ).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private boolean harFortsattFrist(Behandling behandling) {
        return SaksbehandlingsfristUtleder
            .finnUtleder(behandling, fristUtledere)
            .utledFrist(behandling)
            .isPresent();
    }
}
