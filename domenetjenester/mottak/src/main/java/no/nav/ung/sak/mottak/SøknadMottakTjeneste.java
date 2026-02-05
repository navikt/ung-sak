package no.nav.ung.sak.mottak;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;

public interface SøknadMottakTjeneste {

    Fagsak finnEksisterendeFagsak(FagsakYtelseType ytelseType,
                                  AktørId søkerAktørId);

    Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, LocalDate startDato, LocalDate sluttDato);

    Fagsak finnEllerOpprettFagsakForIkkeDigitalBruker(FagsakYtelseType ytelseType,
                                                             AktørId søkerAktørId,
                                                             LocalDate startDato,
                                                             LocalDate sluttDato);

    static SøknadMottakTjeneste finnTjeneste(Instance<SøknadMottakTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(SøknadMottakTjeneste.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType + " har " + instances.stream().map(it->it.getClass()).toList()));
    }
}
