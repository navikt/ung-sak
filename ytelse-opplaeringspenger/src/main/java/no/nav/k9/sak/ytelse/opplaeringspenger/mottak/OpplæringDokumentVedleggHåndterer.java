package no.nav.k9.sak.ytelse.opplaeringspenger.mottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Datotype;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalposttype;
import no.nav.k9.felles.integrasjon.saf.LogiskVedleggResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PersonRepository;

@Dependent
public class OpplæringDokumentVedleggHåndterer {

    private static final Logger log = LoggerFactory.getLogger(OpplæringDokumentVedleggHåndterer.class);
    private static final List<String> DOKUMENTASJON_AV_OPPLÆRING_BREVKODER = List.of(Brevkode.DOKUMENTASJON_AV_OPPLÆRING_KODE, Brevkode.LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING_KODE);

    private final OpplæringDokumentRepository opplæringDokumentRepository;
    private final PersonRepository personRepository;
    private final SafTjeneste safTjeneste;

    @Inject
    public OpplæringDokumentVedleggHåndterer(OpplæringDokumentRepository opplæringDokumentRepository,
                                             PersonRepository personRepository,
                                             SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
        this.opplæringDokumentRepository = opplæringDokumentRepository;
        this.personRepository = personRepository;
    }

    public void leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(Behandling behandling, JournalpostId journalpostId, LocalDateTime mottattidspunkt) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection()
            .kanal()
            .journalposttype()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection()
                    .variantformat()
                    .filnavn()
                    .filtype(null)
                    .saksbehandlerHarTilgang())
                .logiskeVedlegg(new LogiskVedleggResponseProjection()
                    .tittel()))
            .datoOpprettet()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype());
        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);

        log.info("Fant {} vedlegg på søknad", journalpost.getDokumenter().size());
        for (DokumentInfo dokumentInfo : journalpost.getDokumenter()) {
            if (!DOKUMENTASJON_AV_OPPLÆRING_BREVKODER.contains(dokumentInfo.getBrevkode())) {
                continue;
            }

            if (opplæringDokumentRepository.finnesDokument(journalpostId, dokumentInfo.getDokumentInfoId())) {
                log.warn("Tidligere innsendt dokument har blitt sendt inn på nytt -- dette skyldes trolig feil hos avsender. Journalpost: " + journalpostId + ", DokumentInfo: " + dokumentInfo.getDokumentInfoId());
                continue;
            }

            final OpplæringDokument dokument = new OpplæringDokument(
                journalpostId,
                dokumentInfo.getDokumentInfoId(),
                OpplæringDokumentType.DOKUMENTASJON_AV_OPPLÆRING,
                behandling.getUuid(),
                behandling.getFagsak().getSaksnummer(),
                personRepository.hentEllerLagrePerson(behandling.getFagsak().getAktørId()),
                utledMottattDato(journalpost),
                mottattidspunkt);
            opplæringDokumentRepository.lagre(dokument);
        }
    }

    private LocalDate utledMottattDato(Journalpost journalpost) {
        final LocalDate mottattDato;
        if (journalpost.getJournalposttype() == Journalposttype.I) {
            mottattDato = hentRelevantDato(journalpost, Datotype.DATO_REGISTRERT);
        } else {
            mottattDato = hentRelevantDato(journalpost, Datotype.DATO_JOURNALFOERT);
        }
        return mottattDato;
    }

    private LocalDate hentRelevantDato(Journalpost journalpost, Datotype datotype) {
        if (journalpost.getRelevanteDatoer() == null) {
            return LocalDate.now();
        }
        return journalpost.getRelevanteDatoer()
            .stream()
            .filter(d -> d.getDatotype() == datotype)
            .findFirst()
            .orElseThrow()
            .getDato()
            .toLocalDate();
    }
}
