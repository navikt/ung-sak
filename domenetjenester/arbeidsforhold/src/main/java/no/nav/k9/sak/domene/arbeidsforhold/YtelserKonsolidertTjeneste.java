package no.nav.k9.sak.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.TilgrensendeYtelserDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class YtelserKonsolidertTjeneste {

    private FagsakRepository fagsakRepository;

    YtelserKonsolidertTjeneste() {
        // CDI
    }

    @Inject
    public YtelserKonsolidertTjeneste(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }


    /** Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til behandling i VL (som ennå ikke har vedtak). */
    public List<TilgrensendeYtelserDto> utledYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Optional<Set<FagsakYtelseType>> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
            .filter(ytelse -> !inkluder.isPresent() || inkluder.get().contains(ytelse.getYtelseType()))
            .collect(Collectors.toList());
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        Set<Saksnummer> saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        LocalDate iDag = LocalDate.now();
        Set<FagsakStatus> statuser = Set.of(FagsakStatus.OPPRETTET, FagsakStatus.UNDER_BEHANDLING);
        List<TilgrensendeYtelserDto> resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
            .filter(sak -> !inkluder.isPresent() || inkluder.get().contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
            .filter(sak -> statuser.contains(sak.getStatus()))
            .map(sak -> BehandlingRelaterteYtelserMapper.mapFraFagsak(sak, iDag))
            .collect(Collectors.toList());

        resultat.addAll(resultatÅpen);
        return resultat;
    }

}
