package no.nav.ung.sak.historikk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

/** RequestScoped fordi HistorikkInnslagTekstBuilder inneholder state og denne deles på tvers av AksjonspunktOppdaterere. */
@RequestScoped
public class HistorikkTjenesteAdapter {
    private HistorikkinnslagRepository historikkinnslagRepository;
    private HistorikkInnslagKonverter historikkinnslagKonverter;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkTjenesteAdapter() {
        // for CDI proxy
    }

    @Inject
    public HistorikkTjenesteAdapter(HistorikkinnslagRepository historikkinnslagRepository,
                                    HistorikkInnslagKonverter historikkinnslagKonverter,
                                    DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.historikkinnslagKonverter = historikkinnslagKonverter;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public List<HistorikkinnslagDto> mapTilDto(List<Historikkinnslag> historikkinnslagList, Saksnummer saksnummer) {

        var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSak(saksnummer).stream().map(ArkivJournalPost::getJournalpostId).toList();

        return historikkinnslagList.stream()
            .map(historikkinnslag -> historikkinnslagKonverter.map(historikkinnslag, journalPosterForSak))
            .sorted()
            .collect(Collectors.toList());
    }

    public List<Historikkinnslag> finnHistorikkInnslag(Saksnummer saksnummer){
        var liste = new ArrayList<>(historikkinnslagRepository.hent(saksnummer));
        Collections.sort(liste, Comparator.comparing(Historikkinnslag::getOpprettetTidspunkt));
        return liste;
    }

}
