package no.nav.ung.fordel.handler;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.ung.kodeverk.dokument.FordelBehandlingType;
import no.nav.ung.fordel.repo.MeldingRepository;
import no.nav.ung.fordel.repo.MottattMeldingEntitet;


@Dependent
public class MottattMeldingTjeneste {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    private MeldingRepository meldingRepository;

    protected MottattMeldingTjeneste() {
        // for test/mocking
    }

    @Inject
    public MottattMeldingTjeneste(MeldingRepository meldingRepository) {
        this.meldingRepository = meldingRepository;
    }

    void initLogContext(MottattMelding mm) {
        // Utvider felt x_prosess med mer kontekst i tilfelle feil. Dette ryddes automatisk av ProsessTask etter kjøring.

        Optional.ofNullable(mm.getTema()).ifPresent(s -> leggTilMdc("tema", s.getKode()));
        Optional.ofNullable(mm.getBehandlingTema()).ifPresent(s -> leggTilMdc("behandlingTema", s.getKode()));
        Optional.ofNullable(mm.getBrevkode()).ifPresent(s -> leggTilMdc("brevKode", s));

        Optional.ofNullable(mm.getJournalPostId()).ifPresent(s -> leggTilMdc("journalpostId", s.getVerdi()));
        Optional.ofNullable(mm.getArkivId()).ifPresent(s -> leggTilMdc("arkivId", s));
        mm.getForsendelseId().ifPresent(s -> leggTilMdc("forsendelseId", s.toString()));
        mm.getForsendelseMottattTidspunkt().ifPresent(s -> leggTilMdc("forsendelseMottattTidspunkt", s.toString()));
        Optional.ofNullable(mm.getSøknadId()).ifPresent(s -> leggTilMdc("søknadId", s));

        mm.getSaksnummer().ifPresent(s -> leggTilMdc("saksnummer", s));
        mm.getYtelseType().ifPresent(s -> leggTilMdc("ytelseType", s.getKode()));
        mm.getBehandlingType().ifPresent(s -> leggTilMdc("behandlingType", s.getKode()));

    }

    private void leggTilMdc(String key, String val) {
        LOG_CONTEXT.add(key, val);
    }

    void oppdaterMottattMelding(MottattMelding mm) {
        if (mm.getJournalPostId() == null) {
            return; // not ready for this
        }
        String journalpostId = mm.getJournalPostId().getVerdi();
        String tema = mm.getTema() == null ? null : mm.getTema().getKode();
        String behandlingstema = mm.getBehandlingTema() == null ? null : mm.getBehandlingTema().getKode();

        var entitet = meldingRepository
            .finnMottattMelding(journalpostId)
            .orElse(new MottattMeldingEntitet(journalpostId));

        // oppdaterer i tilfelle tilkommet nye verdier
        Optional.ofNullable(tema).ifPresent(entitet::setTema);
        Optional.ofNullable(behandlingstema).ifPresent(entitet::setBehandlingstema);

        mm.getBehandlingType().map(FordelBehandlingType::getKode).ifPresent(entitet::setBehandlingstype);
        Optional.ofNullable(mm.getBrevkode()).ifPresent(entitet::setBrevkode);
        Optional.ofNullable(mm.getSøknadId()).ifPresent(entitet::setSøknadId);
        mm.getPayloadAsString().ifPresent(entitet::setPayload);

        meldingRepository.lagre(entitet);
    }

}
