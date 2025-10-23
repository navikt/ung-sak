package no.nav.ung.sak.kabal.task;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.Vurderingresultat;
import no.nav.ung.sak.kabal.kontrakt.KabalRequestv4;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.RolleType;

import java.util.Collections;
import java.util.List;

@Dependent
public class KabalRequestMapperV4 {

    public KabalRequestv4 map(Behandling behandling, PersonIdent personIdent, KlageUtredningEntitet klageUtredning) {
        var klagendePart = klageUtredning.getKlagendePart();
        var opprinneligBehandlendeEnhet = behandling.getBehandlendeEnhet();
        var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        var ytelseType = behandling.getFagsakYtelseType();

        KabalRequestv4.OversendtKlager klager = null;

        if (klagendePart.isPresent()) {
            if (klagendePart.get().rolleType == RolleType.ARBEIDSGIVER) {
                var orgNr = klagendePart.get().identifikasjon;
                var arbeidsgiverId = new KabalRequestv4.OversendtPartId("VIRKSOMHET", orgNr);
                klager = new KabalRequestv4.OversendtKlager(arbeidsgiverId);
            } else {
                var personId = new KabalRequestv4.OversendtPartId("PERSON", personIdent.getIdent());
                klager = new KabalRequestv4.OversendtKlager(personId);
            }
        }

        var opprettetDato = behandling.getOpprettetDato().toLocalDate();

        var personId = new KabalRequestv4.OversendtPartId("PERSON", personIdent.getIdent());
        var sakenGjelder = new KabalRequestv4.OversendtSakenGjelder(personId);

        // https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/YtelseTilHjemler.kt
        var hjemler = klageUtredning.hentKlagevurdering(KlageVurdertAv.VEDTAKSINSTANS)
            .map(KlageVurderingEntitet::getKlageresultat)
            .map(Vurderingresultat::getHjemmel)
            .map((hjemmel) -> List.of(hjemmel.getKode()))
            .orElse(Collections.emptyList());

        var oversendtSak = new KabalRequestv4.OversendtSak(saksnummer, Fagsystem.UNG_SAK.getKode());

        List<String> tilknyttedeJournalposter = Collections.emptyList();

        var kildereferanse = behandling.getUuid().toString(); // UUID på klage-behandling
        var type = "KLAGE"; // UUID på klage-behandling

        return new KabalRequestv4(
            behandling.getUuid(),
            type,
            klager,
            sakenGjelder,
            oversendtSak,
            kildereferanse,
            List.of("FVL_31"), // Påkrevd. Eneste gyldige hjemmel lagt inn i kabal pt. // hjemler,
            opprinneligBehandlendeEnhet,
            tilknyttedeJournalposter,
            opprettetDato,
            mapK9YtelseTilKabal(ytelseType)
        );
    }

    private String mapK9YtelseTilKabal(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case UNGDOMSYTELSE -> "UNG_UNG";
            default -> FagsakYtelseType.UDEFINERT.getKode();
        };
    }
}
