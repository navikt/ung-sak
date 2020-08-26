package no.nav.k9.sak.historikk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.k9.sak.typer.Saksnummer;

/** RequestScoped fordi HistorikkInnslagTekstBuilder inneholder state og denne deles på tvers av AksjonspunktOppdaterere. */
@RequestScoped
public class HistorikkTjenesteAdapter {
    private HistorikkRepository historikkRepository;
    private HistorikkInnslagTekstBuilder builder;
    private HistorikkInnslagKonverter historikkinnslagKonverter;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkTjenesteAdapter() {
        // for CDI proxy
    }

    @Inject
    public HistorikkTjenesteAdapter(HistorikkRepository historikkRepository,
                                    HistorikkInnslagKonverter historikkinnslagKonverter,
                                    DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.historikkinnslagKonverter = historikkinnslagKonverter;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.builder = new HistorikkInnslagTekstBuilder();
    }

    public List<HistorikkinnslagDto> mapTilDto(List<Historikkinnslag> historikkinnslagList, Saksnummer saksnummer) {

        List<ArkivJournalPost> journalPosterForSak;
        journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSak(saksnummer);

        return historikkinnslagList.stream()
            .map(historikkinnslag -> historikkinnslagKonverter.mapFra(historikkinnslag, journalPosterForSak))
            .sorted()
            .collect(Collectors.toList());
    }

    public List<Historikkinnslag> finnHistorikkInnslag(Saksnummer saksnummer){
        var liste = new ArrayList<>(historikkRepository.hentHistorikkForSaksnummer(saksnummer));
        Collections.sort(liste, Historikkinnslag.COMP_REKKEFØLGE);
        return liste;
    }

    /**
     * IKKE BRUK DENNE.
     * Kall på tekstBuilder() for å få HistorikkInnslagTekstBuilder.  Deretter opprettHistorikkInslag når ferdig
     * @param historikkinnslag
     */
    @Deprecated
    public void lagInnslag(Historikkinnslag historikkinnslag) {
        historikkRepository.lagre(historikkinnslag);
    }

    public HistorikkInnslagTekstBuilder tekstBuilder() {
        return builder;
    }


    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType) {
        opprettHistorikkInnslag(behandlingId, hisType, HistorikkAktør.SAKSBEHANDLER);
    }

    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType, HistorikkAktør historikkAktør) {
        if (!builder.getHistorikkinnslagDeler().isEmpty() || builder.antallEndredeFelter() > 0 ||
            builder.getErBegrunnelseEndret() || builder.getErGjeldendeFraSatt()) {

            Historikkinnslag innslag = new Historikkinnslag();

            builder.medHendelse(hisType);
            innslag.setAktør(historikkAktør);
            innslag.setType(hisType);
            innslag.setBehandlingId(behandlingId);
            builder.build(innslag);

            resetBuilder();

            lagInnslag(innslag);
        }
    }

    private void resetBuilder() {
        builder = new HistorikkInnslagTekstBuilder();
    }
}
