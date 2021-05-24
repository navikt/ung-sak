package no.nav.k9.sak.hendelsemottak.resttjenester;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.hendelsemottak.tjenester.FordelHendelseTjeneste;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.typer.AktørId;


@ApplicationScoped
@Transactional
public class FordelHendelseRestTjeneste {
    // TODO: Flytt til webapp

    private FordelHendelseTjeneste fordelHendelseTjeneste;

    public FordelHendelseRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelHendelseRestTjeneste(FordelHendelseTjeneste fordelHendelseTjeneste) {
        this.fordelHendelseTjeneste = fordelHendelseTjeneste;
    }


    public Set<Fagsak> finnPåvirkedeFagsaker(AktørId aktørId, Hendelse hendelse) {
        return fordelHendelseTjeneste.finnFagsakerTilVurdering(aktørId, hendelse).keySet();
    }

    public void mottaHendelse(AktørId aktørId, Hendelse hendelse) {
        fordelHendelseTjeneste.mottaHendelse(aktørId, hendelse);
    }
}
