package no.nav.ung.sak.kabal.task;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.Vurderingresultat;
import no.nav.ung.sak.kabal.kontrakt.KabalRequest;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.RolleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

@Dependent
public class KabalRequestMapper {

    public KabalRequest map(Behandling behandling, PersonIdent personIdent, KlageUtredningEntitet klageUtredning) {
        KabalRequest request = new KabalRequest();
        var opprettetDato = behandling.getOpprettetDato().toLocalDate().toString();
        request.setBehandlingUuid(behandling.getUuid());
        var klagendePart = klageUtredning.getKlagendePart();
        var opprinneligBehandlendeEnhet = behandling.getBehandlendeEnhet();
        var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        var ytelseType = behandling.getFagsakYtelseType();

        if (klagendePart.isPresent() && klagendePart.get().rolleType == RolleType.ARBEIDSGIVER) {
            var orgNr = klagendePart.get().identifikasjon;
            var arbeidsgiverId = new KabalRequest.OversendtPartId("VIRKSOMHET", orgNr);
            var klage = new KabalRequest.OversendtKlager(arbeidsgiverId, null);
            request.setKlager(klage);

            var personId = new KabalRequest.OversendtPartId("PERSON", personIdent.getIdent());
            var sakenGjelder = new KabalRequest.OversendtSakenGjelder(personId, false);
            request.setSakenGjelder(sakenGjelder);
        } else {
            var personId = new KabalRequest.OversendtPartId("PERSON", personIdent.getIdent());
            var klage = new KabalRequest.OversendtKlager(personId, null);
            request.setKlager(klage);
        }

        request.setAvsenderEnhet(opprinneligBehandlendeEnhet);
        var avsenderSaksbehandlerIdent = behandling.getAnsvarligSaksbehandler().toUpperCase();
        request.setAvsenderSaksbehandlerIdent(avsenderSaksbehandlerIdent);
        request.setInnsendtTilNav(opprettetDato);
        request.setKilde("UNG");
        request.setKildeReferanse(behandling.getUuid().toString());

        // https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/YtelseTilHjemler.kt
        klageUtredning.hentKlagevurdering(KlageVurdertAv.VEDTAKSINSTANS)
            .map(KlageVurderingEntitet::getKlageresultat)
            .map(Vurderingresultat::getHjemmel)
            .ifPresent(hjemmel -> {
                var hjemler = new ArrayList<String>();
                hjemler.add(hjemmel.getKode());

                request.setHjemler(hjemler);
            });

        var oversendtSak = new KabalRequest.OversendtSak(saksnummer, Fagsystem.UNG_SAK.getKode());
        request.setOversendtSak(oversendtSak);

        request.setTilknyttedeJournalposter(Collections.emptyList());
        request.setMottattFoersteinstans(opprettetDato);
        request.setType("KLAGE");
        request.setDvhReferanse(behandling.getUuid().toString()); // UUID på klage-behandling
        request.setYtelse(mapK9YtelseTilKabal(ytelseType));
        request.setOversendtKaDato(LocalDateTime.now().toString());

        return request;
    }

    private String mapK9YtelseTilKabal(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case UNGDOMSYTELSE -> FagsakYtelseType.UNGDOMSYTELSE.getKode();
            default -> FagsakYtelseType.UDEFINERT.getKode();
        };
    }
}
