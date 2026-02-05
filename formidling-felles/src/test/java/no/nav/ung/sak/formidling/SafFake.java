package no.nav.ung.sak.formidling;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;
import no.nav.k9.felles.integrasjon.saf.Arkivsaksystem;
import no.nav.k9.felles.integrasjon.saf.AvsenderMottaker;
import no.nav.k9.felles.integrasjon.saf.AvsenderMottakerIdType;
import no.nav.k9.felles.integrasjon.saf.Bruker;
import no.nav.k9.felles.integrasjon.saf.BrukerIdType;
import no.nav.k9.felles.integrasjon.saf.Datotype;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.Dokumentoversikt;
import no.nav.k9.felles.integrasjon.saf.DokumentoversiktBrukerQueryRequest;
import no.nav.k9.felles.integrasjon.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.k9.felles.integrasjon.saf.DokumentoversiktResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Dokumentstatus;
import no.nav.k9.felles.integrasjon.saf.Dokumentvariant;
import no.nav.k9.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalposttype;
import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.felles.integrasjon.saf.LogiskVedlegg;
import no.nav.k9.felles.integrasjon.saf.RelevantDato;
import no.nav.k9.felles.integrasjon.saf.Saf;
import no.nav.k9.felles.integrasjon.saf.Sak;
import no.nav.k9.felles.integrasjon.saf.Sakstype;
import no.nav.k9.felles.integrasjon.saf.SkjermingType;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.felles.integrasjon.saf.TilknyttedeJournalposterQueryRequest;
import no.nav.k9.felles.integrasjon.saf.Variantformat;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.typer.JournalpostId;
import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafFake implements Saf {

    public static final JournalpostId JOURNAL_ID = new JournalpostId("24");
    public static final String DOKUMENT_ID = "55";

    private Map<String, Journalpost> journalpostMap = new HashMap<>();

    public void leggTilJournalpost(JournalpostFake jp) {
        journalpostMap.put(jp.journalpostId, byggJournalpost(jp));
    }

    @Override
    public <T extends GraphQLResult<?>> T query(GraphQLOperationRequest q, GraphQLResponseProjection p, Class<T> clazz) {
        throw new NotImplementedException();
    }

    @Override
    public Dokumentoversikt dokumentoversiktFagsak(DokumentoversiktFagsakQueryRequest query, DokumentoversiktResponseProjection projection) {
        throw new NotImplementedException();
    }

    @Override
    public Dokumentoversikt dokumentoversiktBruker(DokumentoversiktBrukerQueryRequest queryRequest, DokumentoversiktResponseProjection projection) {
        throw new NotImplementedException();
    }

    @Override
    public Journalpost hentJournalpostInfo(JournalpostQueryRequest query, JournalpostResponseProjection projection) {
        String journalpostId = (String) query.getInput().get("journalpostId");
        return journalpostMap.get(journalpostId);
    }


    private Journalpost byggJournalpost(JournalpostFake jp) {
        var journalpost = new Journalpost();
        journalpost.setJournalpostId(jp.journalpostId);
        journalpost.setTittel("tittel");
        journalpost.setJournalposttype(Journalposttype.U);
        journalpost.setJournalstatus(Journalstatus.FERDIGSTILT);
        journalpost.setKanal(Kanal.NAV_NO);
        journalpost.setKanalnavn("nav.no");
        journalpost.setTema(Tema.AAP);
        journalpost.setBehandlingstema("behandlingstema");
        journalpost.setSak(new Sak("arkivsaksystem", Arkivsaksystem.GSAK, LocalDateTime.now(), "fagsakId", "fagsaksystem", Sakstype.FAGSAK, Tema.UNG));
        journalpost.setBruker(new Bruker("id", BrukerIdType.AKTOERID));
        journalpost.setAvsenderMottaker(new AvsenderMottaker("fnr", AvsenderMottakerIdType.FNR, "Navn", "Land", true));
        journalpost.setJournalfoerendeEnhet("journalstatus");
        journalpost.setDokumenter(List.of(byggDokumentInfo(jp)));
        journalpost.setRelevanteDatoer(List.of(
            new RelevantDato(LocalDateTime.now(), Datotype.DATO_JOURNALFOERT),
            new RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)));
        journalpost.setEksternReferanseId("eksternReferanseId");

        return journalpost;
    }

    private DokumentInfo byggDokumentInfo(JournalpostFake input) {
        return new DokumentInfo(input.dokumentId, "tittel", input.dokumentMalType().getKode(), Dokumentstatus.FERDIGSTILT, LocalDateTime.now(), "origJpId", SkjermingType.POL.name(),
            List.of(new LogiskVedlegg("id", "tittel")),
            List.of(new Dokumentvariant(Variantformat.ORIGINAL, "filnavn", "fluuid", ArkivFilType.PDF.name(), true, SkjermingType.POL)));
    }


    @Override
    public List<Journalpost> hentTilknyttedeJournalposter(TilknyttedeJournalposterQueryRequest query, JournalpostResponseProjection projection) {
        throw new NotImplementedException();
    }

    @Override
    public byte[] hentDokument(HentDokumentQuery q) {
        throw new NotImplementedException();
    }

    public record JournalpostFake(String journalpostId, String dokumentId, DokumentMalType dokumentMalType) {

    }
}
