package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnSøknadMottaker implements SøknadMottakTjeneste<FrisinnSøknadInnsending> {

    private SøknadDokumentmottaker dokumentMottaker;

    protected FrisinnSøknadMottaker() {
        // for proxy
    }

    @Inject
    public FrisinnSøknadMottaker(SøknadDokumentmottaker dokumentMottaker) {
        this.dokumentMottaker = dokumentMottaker;
    }

    @Override
    public Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, FrisinnSøknadInnsending søknadInnsending) {
        return dokumentMottaker.mottaSøknad(saksnummer, journalpostId, søknadInnsending.getSøknad());
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, LocalDate startDato) {
        return dokumentMottaker.finnEllerOpprett(ytelseType, søkerAktørId, pleietrengendeAktørId, startDato);
    }

}
